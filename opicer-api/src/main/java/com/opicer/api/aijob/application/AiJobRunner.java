package com.opicer.api.aijob.application;

import com.opicer.api.aijob.application.AiJobProcessor.Outcome;
import com.opicer.api.aijob.domain.AiJob;
import com.opicer.api.aijob.domain.AiJobStatus;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.aijob.infrastructure.AiJobRepository;
import com.opicer.api.config.WorkerProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Claims due jobs and runs them. Claiming happens in a short transaction ({@code FOR UPDATE SKIP LOCKED}, then
 * RUNNING), so several workers never take the same job; the work itself runs outside any transaction.
 * {@link #runOnce()} is what the scheduler calls, and what tests call directly.
 */
@Service
public class AiJobRunner {

	private static final Logger log = LoggerFactory.getLogger(AiJobRunner.class);

	private final AiJobRepository repository;
	private final WorkerProperties properties;
	private final Map<AiJobType, AiJobProcessor> processors = new EnumMap<>(AiJobType.class);
	private final TransactionTemplate tx;
	private final Clock clock;
	private final String node = "worker-" + UUID.randomUUID().toString().substring(0, 8);

	@Autowired
	public AiJobRunner(AiJobRepository repository, WorkerProperties properties, List<AiJobProcessor> processors,
		PlatformTransactionManager transactionManager) {
		this(repository, properties, processors, transactionManager, Clock.systemUTC());
	}

	AiJobRunner(AiJobRepository repository, WorkerProperties properties, List<AiJobProcessor> processors,
		PlatformTransactionManager transactionManager, Clock clock) {
		this.repository = repository;
		this.properties = properties;
		processors.forEach(p -> this.processors.put(p.type(), p));
		this.tx = new TransactionTemplate(transactionManager);
		this.clock = clock;
	}

	/** @return how many jobs were claimed and run */
	public int runOnce() {
		Instant now = clock.instant();
		Integer released = tx.execute(status -> repository.releaseStale(
			now.minus(Duration.ofMinutes(properties.getStaleAfterMinutes())), now, AiJobStatus.READY, AiJobStatus.RUNNING));
		if (released != null && released > 0) {
			log.warn("[AiJob] released {} stale job(s)", released);
		}
		List<AiJob> claimed = tx.execute(status -> {
			List<AiJob> jobs = repository.lockDueJobs(properties.getBatchSize());
			jobs.forEach(job -> job.claim(node, now));
			return jobs;
		});
		for (AiJob job : claimed) {
			run(job);
		}
		return claimed.size();
	}

	private void run(AiJob job) {
		boolean finalAttempt = job.getAttempts() >= properties.getMaxAttempts();
		Outcome outcome;
		String error = null;
		AiJobProcessor processor = processors.get(job.getType());
		if (processor == null) {
			outcome = Outcome.RETRY;
			error = "no processor for " + job.getType();
		} else {
			try {
				outcome = processor.process(job.getSubjectId(), finalAttempt);
			} catch (RuntimeException e) {
				log.error("[AiJob] {} {} failed unexpectedly", job.getType(), job.getSubjectId(), e);
				outcome = Outcome.RETRY;
				error = e.getClass().getSimpleName() + ": " + e.getMessage();
			}
		}
		boolean gaveUp = settle(job.getId(), outcome, error == null ? "transient failure" : error);
		if (gaveUp && processor != null) {
			try {
				processor.abandon(job.getSubjectId());
			} catch (RuntimeException e) {
				log.error("[AiJob] abandoning {} {} failed", job.getType(), job.getSubjectId(), e);
			}
		}
	}

	/** @return true when the job was given up (FAILED) */
	private boolean settle(UUID jobId, Outcome outcome, String error) {
		Instant now = clock.instant();
		Boolean gaveUp = tx.execute(status -> repository.findById(jobId).map(job -> {
			if (outcome == Outcome.COMPLETED) {
				job.succeed(now);
				return false;
			}
			if (job.getAttempts() >= properties.getMaxAttempts()) {
				job.fail(error, now);
				return true;
			}
			job.retryAt(now.plus(backoff(job.getAttempts())), error, now);
			return false;
		}).orElse(false));
		return Boolean.TRUE.equals(gaveUp);
	}

	Duration backoff(int failedAttempts) {
		List<Integer> steps = properties.getBackoffSeconds();
		int index = Math.min(Math.max(failedAttempts, 1), steps.size()) - 1;
		return Duration.ofSeconds(steps.get(index));
	}
}
