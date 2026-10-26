package com.opicer.api.practice.application;

import com.opicer.api.aijob.application.AiJobProcessor;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.config.WorkerProperties;
import com.opicer.api.evaluation.application.AnswerEvaluationResult;
import com.opicer.api.evaluation.application.AnswerEvaluator;
import com.opicer.api.evaluation.application.AnswerEvaluator.AnswerToEvaluate;
import com.opicer.api.evaluation.application.EvaluationDocuments;
import com.opicer.api.evaluation.application.EvaluationFailure;
import com.opicer.api.evaluation.application.EvaluationGuideNote;
import com.opicer.api.evaluation.domain.AnswerAssessment;
import com.opicer.api.evaluation.domain.LevelAggregator;
import com.opicer.api.evaluation.domain.LevelPolicy;
import com.opicer.api.evaluation.domain.SessionReport;
import com.opicer.api.evaluationguide.application.EvaluationGuideQueryService;
import com.opicer.api.practice.domain.AnswerEvaluationStatus;
import com.opicer.api.practice.domain.PracticeAnswer;
import com.opicer.api.practice.domain.PracticeSession;
import com.opicer.api.practice.domain.SessionEvaluationStatus;
import com.opicer.api.practice.infrastructure.PracticeAnswerRepository;
import com.opicer.api.practice.infrastructure.PracticeSessionRepository;
import com.opicer.api.question.domain.Question;
import com.opicer.api.question.infrastructure.QuestionRepository;
import com.opicer.api.shared.domain.OpicLevel;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Evaluates every pending answer of a practice session, then writes the session report.
 * Database work happens in short transactions; model calls run outside them, a few in parallel. Each answer is
 * saved as soon as it finishes, so a retry re-evaluates only the answers that failed transiently.
 */
@Component
public class SessionEvaluationProcessor implements AiJobProcessor {

	private static final Logger log = LoggerFactory.getLogger(SessionEvaluationProcessor.class);

	private final PracticeSessionRepository sessionRepository;
	private final PracticeAnswerRepository answerRepository;
	private final QuestionRepository questionRepository;
	private final AnswerEvaluator evaluator;
	private final EvaluationGuideQueryService guides;
	private final TransactionTemplate tx;
	private final ExecutorService executor;
	private final LevelAggregator aggregator = new LevelAggregator(LevelPolicy.DEFAULT);

	public SessionEvaluationProcessor(PracticeSessionRepository sessionRepository,
		PracticeAnswerRepository answerRepository, QuestionRepository questionRepository, AnswerEvaluator evaluator,
		EvaluationGuideQueryService guides, PlatformTransactionManager transactionManager,
		WorkerProperties workerProperties) {
		this.sessionRepository = sessionRepository;
		this.answerRepository = answerRepository;
		this.questionRepository = questionRepository;
		this.evaluator = evaluator;
		this.guides = guides;
		this.tx = new TransactionTemplate(transactionManager);
		this.executor = Executors.newFixedThreadPool(Math.max(1, workerProperties.getConcurrency()), r -> {
			Thread thread = new Thread(r, "answer-evaluation");
			thread.setDaemon(true);
			return thread;
		});
	}

	@PreDestroy
	void shutdown() {
		executor.shutdownNow();
	}

	@Override
	public AiJobType type() {
		return AiJobType.SESSION_EVALUATION;
	}

	@Override
	public Outcome process(UUID sessionId, boolean finalAttempt) {
		Snapshot snapshot = tx.execute(status -> start(sessionId));
		if (snapshot == null) {
			return Outcome.COMPLETED; // session deleted meanwhile: nothing to do
		}

		List<CompletableFuture<Boolean>> runs = snapshot.pending().stream()
			.map(answer -> CompletableFuture.supplyAsync(() -> evaluate(answer, snapshot.guides(), finalAttempt), executor))
			.toList();
		boolean transientFailure = runs.stream().map(CompletableFuture::join).anyMatch(stillPending -> stillPending);
		if (transientFailure && !finalAttempt) {
			return Outcome.RETRY;
		}
		tx.executeWithoutResult(status -> finish(sessionId));
		return Outcome.COMPLETED;
	}

	/** The runner gave up: whatever is still pending failed, and the session gets whatever report it can. */
	@Override
	public void abandon(UUID sessionId) {
		tx.executeWithoutResult(status -> {
			for (PracticeAnswer answer : answerRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId)) {
				if (answer.getEvaluationStatus() == AnswerEvaluationStatus.PENDING) {
					answer.markEvaluationFailed();
				}
			}
		});
		tx.executeWithoutResult(status -> finish(sessionId));
	}

	/**
	 * Marks the session running and snapshots what to evaluate, plus the active admin guides (read once here so
	 * parallel answer evaluations do not each take a connection); null when the session no longer exists.
	 */
	private Snapshot start(UUID sessionId) {
		PracticeSession session = sessionRepository.findById(sessionId).orElse(null);
		if (session == null) {
			return null;
		}
		session.startEvaluation();
		OpicLevel target = session.getTargetLevel() == null
			? PracticeSessionCommandService.DEFAULT_TARGET_LEVEL
			: session.getTargetLevel();
		List<PendingAnswer> pending = new ArrayList<>();
		for (PracticeAnswer answer : answerRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId)) {
			if (answer.getEvaluationStatus() != AnswerEvaluationStatus.PENDING) {
				continue;
			}
			Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
			pending.add(new PendingAnswer(answer.getId(), new AnswerToEvaluate(
				session.getTopicId(),
				answer.getQuestionText(),
				question == null || question.getType() == null ? null : question.getType().name(),
				question == null ? null : question.getStructuralHint(),
				question == null ? List.of() : List.copyOf(question.getKeyExpressions()),
				answer.getTranscript(),
				answer.getDurationSeconds(),
				target
			)));
		}
		return new Snapshot(pending, guides.activeNotes());
	}

	/** @return true when the answer is still pending because of a transient failure */
	private boolean evaluate(PendingAnswer pending, List<EvaluationGuideNote> guides, boolean finalAttempt) {
		try {
			AnswerEvaluationResult result = evaluator.evaluate(pending.answer(), guides);
			save(pending.answerId(), answer -> {
				if (result.skipped()) {
					answer.skipEvaluation();
				} else {
					answer.recordEvaluation(result.evaluationJson(), result.overallLevel(), result.summaryKo(),
						result.targetScript());
				}
			});
			return false;
		} catch (EvaluationFailure failure) {
			log.warn("[Eval] answer {} failed: {} (retryable={})", pending.answerId(), failure.reason(), failure.retryable());
			if (failure.retryable() && !finalAttempt) {
				return true;
			}
			save(pending.answerId(), PracticeAnswer::markEvaluationFailed);
			return false;
		} catch (RuntimeException e) {
			log.error("[Eval] answer {} failed unexpectedly", pending.answerId(), e);
			if (!finalAttempt) {
				return true;
			}
			save(pending.answerId(), PracticeAnswer::markEvaluationFailed);
			return false;
		}
	}

	private void save(UUID answerId, Consumer<PracticeAnswer> change) {
		tx.executeWithoutResult(status -> answerRepository.findById(answerId).ifPresent(change));
	}

	/** Aggregates every evaluated answer (from this and earlier attempts) into the session report. */
	private void finish(UUID sessionId) {
		PracticeSession session = sessionRepository.findById(sessionId).orElse(null);
		if (session == null) {
			return;
		}
		List<AnswerAssessment> assessments = new ArrayList<>();
		int skipped = 0;
		int failed = 0;
		for (PracticeAnswer answer : answerRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId)) {
			if (answer.getEvaluationStatus() == AnswerEvaluationStatus.DONE && answer.getEvaluation() != null) {
				assessments.add(EvaluationDocuments.assessment(answer.getEvaluation()));
			} else if (answer.getEvaluationStatus() == AnswerEvaluationStatus.SKIPPED) {
				skipped++;
			} else if (answer.getEvaluationStatus() == AnswerEvaluationStatus.FAILED) {
				failed++;
			}
		}
		if (assessments.isEmpty()) {
			session.completeEvaluation(SessionEvaluationStatus.FAILED, null, null);
			return;
		}
		OpicLevel target = session.getTargetLevel() == null
			? PracticeSessionCommandService.DEFAULT_TARGET_LEVEL
			: session.getTargetLevel();
		SessionReport report = aggregator.aggregate(assessments, target);
		session.completeEvaluation(failed > 0 ? SessionEvaluationStatus.PARTIAL : SessionEvaluationStatus.DONE,
			report.estimatedLevel(), EvaluationDocuments.report(report, skipped, failed));
	}

	private record PendingAnswer(UUID answerId, AnswerToEvaluate answer) {}

	private record Snapshot(List<PendingAnswer> pending, List<EvaluationGuideNote> guides) {}
}
