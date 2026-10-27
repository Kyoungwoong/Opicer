package com.opicer.api.practice.application;

import com.opicer.api.aijob.application.AiJobProcessor;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.evaluation.application.AnswerEvaluationResult;
import com.opicer.api.evaluation.application.AnswerBatchEvaluator;
import com.opicer.api.evaluation.application.AnswerBatchEvaluator.Pending;
import com.opicer.api.evaluation.application.AnswerEvaluator.AnswerToEvaluate;
import com.opicer.api.evaluation.application.EvaluationGuideNote;
import com.opicer.api.evaluation.domain.EvaluationMode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Coaches every pending answer of a topic-practice session (corrections, expressions, target script; no levels),
 * then settles the session's status.
 * Database work happens in short transactions; model calls run outside them, a few in parallel. Each answer is
 * saved as soon as it finishes, so a retry re-evaluates only the answers that failed transiently.
 */
@Component
public class SessionEvaluationProcessor implements AiJobProcessor {

	private final PracticeSessionRepository sessionRepository;
	private final PracticeAnswerRepository answerRepository;
	private final QuestionRepository questionRepository;
	private final AnswerBatchEvaluator batch;
	private final EvaluationGuideQueryService guides;
	private final TransactionTemplate tx;

	public SessionEvaluationProcessor(PracticeSessionRepository sessionRepository,
		PracticeAnswerRepository answerRepository, QuestionRepository questionRepository, AnswerBatchEvaluator batch,
		EvaluationGuideQueryService guides, PlatformTransactionManager transactionManager) {
		this.sessionRepository = sessionRepository;
		this.answerRepository = answerRepository;
		this.questionRepository = questionRepository;
		this.batch = batch;
		this.guides = guides;
		this.tx = new TransactionTemplate(transactionManager);
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

		boolean transientFailure = batch.evaluate(snapshot.pending(), snapshot.guides(), EvaluationMode.COACH,
			finalAttempt, new AnswerBatchEvaluator.Results() {
				@Override
				public void evaluated(UUID answerId, AnswerEvaluationResult result) {
					save(answerId, answer -> {
						if (result.skipped()) {
							answer.skipEvaluation();
						} else {
							answer.recordEvaluation(result.evaluationJson(), result.overallLevel(), result.summaryKo(),
								result.targetScript());
						}
					});
				}

				@Override
				public void failed(UUID answerId) {
					save(answerId, PracticeAnswer::markEvaluationFailed);
				}
			});
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
		List<Pending> pending = new ArrayList<>();
		for (PracticeAnswer answer : answerRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId)) {
			if (answer.getEvaluationStatus() != AnswerEvaluationStatus.PENDING) {
				continue;
			}
			Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
			pending.add(new Pending(answer.getId(), new AnswerToEvaluate(
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

	private void save(UUID answerId, Consumer<PracticeAnswer> change) {
		tx.executeWithoutResult(status -> answerRepository.findById(answerId).ifPresent(change));
	}

	/**
	 * Topic practice is coaching only: the session gets a final status but no report or level. Counts answers from
	 * this and earlier attempts.
	 */
	private void finish(UUID sessionId) {
		PracticeSession session = sessionRepository.findById(sessionId).orElse(null);
		if (session == null) {
			return;
		}
		int done = 0;
		int failed = 0;
		for (PracticeAnswer answer : answerRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId)) {
			if (answer.getEvaluationStatus() == AnswerEvaluationStatus.DONE) {
				done++;
			} else if (answer.getEvaluationStatus() == AnswerEvaluationStatus.FAILED) {
				failed++;
			}
		}
		SessionEvaluationStatus status = done == 0
			? SessionEvaluationStatus.FAILED
			: failed > 0 ? SessionEvaluationStatus.PARTIAL : SessionEvaluationStatus.DONE;
		session.completeEvaluation(status, null, null);
	}

	private record Snapshot(List<Pending> pending, List<EvaluationGuideNote> guides) {}
}
