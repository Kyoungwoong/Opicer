package com.opicer.api.evaluation.application;

import com.opicer.api.config.WorkerProperties;
import com.opicer.api.evaluation.application.AnswerEvaluator.AnswerToEvaluate;
import com.opicer.api.evaluation.domain.EvaluationMode;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Evaluates a batch of stored answers a few at a time (outside any transaction) and hands each outcome to the
 * caller, which saves it right away. Shared by topic practice and the mock test, so both retry the same way:
 * a transient failure leaves the answer pending unless this is the final attempt.
 */
@Component
public class AnswerBatchEvaluator {

	private static final Logger log = LoggerFactory.getLogger(AnswerBatchEvaluator.class);

	private final AnswerEvaluator evaluator;
	private final ExecutorService executor;

	public AnswerBatchEvaluator(AnswerEvaluator evaluator, WorkerProperties workerProperties) {
		this.evaluator = evaluator;
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

	/** @param itemId the caller's id for the stored answer (practice answer or mock test item) */
	public record Pending(UUID itemId, AnswerToEvaluate answer) {}

	/** Called from worker threads; each call should save in its own short transaction. */
	public interface Results {
		void evaluated(UUID itemId, AnswerEvaluationResult result);

		void failed(UUID itemId);
	}

	/** @return true when at least one answer is still pending after a transient failure */
	public boolean evaluate(List<Pending> pending, List<EvaluationGuideNote> guides, EvaluationMode mode,
		boolean finalAttempt, Results results) {
		List<CompletableFuture<Boolean>> runs = pending.stream()
			.map(item -> CompletableFuture.supplyAsync(() -> evaluateOne(item, guides, mode, finalAttempt, results), executor))
			.toList();
		return runs.stream().map(CompletableFuture::join).anyMatch(stillPending -> stillPending);
	}

	private boolean evaluateOne(Pending item, List<EvaluationGuideNote> guides, EvaluationMode mode,
		boolean finalAttempt, Results results) {
		try {
			results.evaluated(item.itemId(), evaluator.evaluate(item.answer(), guides, mode));
			return false;
		} catch (EvaluationFailure failure) {
			log.warn("[Eval] answer {} failed: {} (retryable={})", item.itemId(), failure.reason(), failure.retryable());
			if (failure.retryable() && !finalAttempt) {
				return true;
			}
			results.failed(item.itemId());
			return false;
		} catch (RuntimeException e) {
			log.error("[Eval] answer {} failed unexpectedly", item.itemId(), e);
			if (!finalAttempt) {
				return true;
			}
			results.failed(item.itemId());
			return false;
		}
	}
}
