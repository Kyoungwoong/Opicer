package com.opicer.api.mocktest.application;

import com.opicer.api.aijob.application.AiJobProcessor;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.evaluation.application.AnswerBatchEvaluator;
import com.opicer.api.evaluation.application.AnswerBatchEvaluator.Pending;
import com.opicer.api.evaluation.application.AnswerEvaluationResult;
import com.opicer.api.evaluation.application.AnswerEvaluator.AnswerToEvaluate;
import com.opicer.api.evaluation.application.EvaluationDocuments;
import com.opicer.api.evaluation.application.EvaluationGuideNote;
import com.opicer.api.evaluation.domain.AnswerAssessment;
import com.opicer.api.evaluation.domain.EvaluationMode;
import com.opicer.api.evaluation.domain.LevelAggregator;
import com.opicer.api.evaluation.domain.LevelPolicy;
import com.opicer.api.evaluation.domain.SessionReport;
import com.opicer.api.evaluationguide.application.EvaluationGuideQueryService;
import com.opicer.api.mocktest.domain.MockTest;
import com.opicer.api.mocktest.domain.MockTestItem;
import com.opicer.api.mocktest.infrastructure.MockTestItemRepository;
import com.opicer.api.mocktest.infrastructure.MockTestRepository;
import com.opicer.api.practice.domain.AnswerEvaluationStatus;
import com.opicer.api.practice.domain.SessionEvaluationStatus;
import com.opicer.api.question.domain.Question;
import com.opicer.api.question.infrastructure.QuestionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Evaluates every answer of a submitted mock test with the full rubric, then writes the holistic report: the
 * estimated level comes from all answers together, not from adding up per-question scores.
 */
@Component
public class MockTestEvaluationProcessor implements AiJobProcessor {

	private final MockTestRepository tests;
	private final MockTestItemRepository items;
	private final QuestionRepository questions;
	private final AnswerBatchEvaluator batch;
	private final EvaluationGuideQueryService guides;
	private final TransactionTemplate tx;
	private final Clock clock;
	private final LevelAggregator aggregator = new LevelAggregator(LevelPolicy.DEFAULT);

	public MockTestEvaluationProcessor(MockTestRepository tests, MockTestItemRepository items,
		QuestionRepository questions, AnswerBatchEvaluator batch, EvaluationGuideQueryService guides,
		PlatformTransactionManager transactionManager, Clock clock) {
		this.tests = tests;
		this.items = items;
		this.questions = questions;
		this.batch = batch;
		this.guides = guides;
		this.tx = new TransactionTemplate(transactionManager);
		this.clock = clock;
	}

	@Override
	public AiJobType type() {
		return AiJobType.MOCK_TEST_EVALUATION;
	}

	@Override
	public Outcome process(UUID mockTestId, boolean finalAttempt) {
		Snapshot snapshot = tx.execute(status -> start(mockTestId));
		if (snapshot == null) {
			return Outcome.COMPLETED; // deleted meanwhile
		}
		boolean transientFailure = batch.evaluate(snapshot.pending(), snapshot.guides(), EvaluationMode.FULL,
			finalAttempt, new AnswerBatchEvaluator.Results() {
				@Override
				public void evaluated(UUID itemId, AnswerEvaluationResult result) {
					save(itemId, item -> {
						if (result.skipped()) {
							item.skipEvaluation();
						} else {
							item.recordEvaluation(result.evaluationJson(), result.overallLevel());
						}
					});
				}

				@Override
				public void failed(UUID itemId) {
					save(itemId, MockTestItem::markEvaluationFailed);
				}
			});
		if (transientFailure && !finalAttempt) {
			return Outcome.RETRY;
		}
		tx.executeWithoutResult(status -> finish(mockTestId));
		return Outcome.COMPLETED;
	}

	@Override
	public void abandon(UUID mockTestId) {
		tx.executeWithoutResult(status -> {
			for (MockTestItem item : items.findByMockTestIdOrderByItemOrderAsc(mockTestId)) {
				if (item.getEvaluationStatus() == AnswerEvaluationStatus.PENDING) {
					item.markEvaluationFailed();
				}
			}
		});
		tx.executeWithoutResult(status -> finish(mockTestId));
	}

	private Snapshot start(UUID mockTestId) {
		MockTest test = tests.findById(mockTestId).orElse(null);
		if (test == null) {
			return null;
		}
		test.startEvaluation();
		List<Pending> pending = new ArrayList<>();
		for (MockTestItem item : items.findByMockTestIdOrderByItemOrderAsc(mockTestId)) {
			if (item.getEvaluationStatus() != AnswerEvaluationStatus.PENDING) {
				continue;
			}
			Question question = questions.findById(item.getQuestionId()).orElse(null);
			pending.add(new Pending(item.getId(), new AnswerToEvaluate(
				item.getTopicId(),
				item.getQuestionText(),
				item.getQuestionType(),
				question == null ? null : question.getStructuralHint(),
				question == null ? List.of() : List.copyOf(question.getKeyExpressions()),
				item.getTranscript(),
				item.getDurationSeconds(),
				test.getTargetLevel()
			)));
		}
		return new Snapshot(pending, guides.activeNotes());
	}

	private void save(UUID itemId, Consumer<MockTestItem> change) {
		tx.executeWithoutResult(status -> items.findById(itemId).ifPresent(change));
	}

	/** One report over every evaluated answer, from this and earlier attempts. */
	private void finish(UUID mockTestId) {
		MockTest test = tests.findById(mockTestId).orElse(null);
		if (test == null) {
			return;
		}
		List<AnswerAssessment> assessments = new ArrayList<>();
		int skipped = 0;
		int failed = 0;
		for (MockTestItem item : items.findByMockTestIdOrderByItemOrderAsc(mockTestId)) {
			if (item.getEvaluationStatus() == AnswerEvaluationStatus.DONE && item.getEvaluation() != null) {
				assessments.add(EvaluationDocuments.assessment(item.getEvaluation()));
			} else if (item.getEvaluationStatus() == AnswerEvaluationStatus.SKIPPED) {
				skipped++;
			} else if (item.getEvaluationStatus() == AnswerEvaluationStatus.FAILED) {
				failed++;
			}
		}
		Instant now = Instant.now(clock);
		if (assessments.isEmpty()) {
			test.completeEvaluation(SessionEvaluationStatus.FAILED, null, null, null, now);
			return;
		}
		SessionReport report = aggregator.aggregate(assessments, test.getTargetLevel());
		test.completeEvaluation(failed > 0 ? SessionEvaluationStatus.PARTIAL : SessionEvaluationStatus.DONE,
			report.estimatedLevel(), EvaluationDocuments.report(report, skipped, failed), null, now);
	}

	private record Snapshot(List<Pending> pending, List<EvaluationGuideNote> guides) {}
}
