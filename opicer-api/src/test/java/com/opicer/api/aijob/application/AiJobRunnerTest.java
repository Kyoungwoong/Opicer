package com.opicer.api.aijob.application;

import com.opicer.api.aijob.application.AiJobProcessor.Outcome;
import com.opicer.api.aijob.domain.AiJob;
import com.opicer.api.aijob.domain.AiJobStatus;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.aijob.infrastructure.AiJobRepository;
import com.opicer.api.config.WorkerProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Claiming, backoff and failure handling on real Postgres, with a scripted processor. */
@SpringBootTest
class AiJobRunnerTest {

	@Autowired
	private AiJobRepository repository;

	@Autowired
	private AiJobCommandService commandService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbc;

	private final WorkerProperties properties = new WorkerProperties();
	private final List<UUID> processed = new ArrayList<>();
	private Function<UUID, Outcome> behaviour = subject -> Outcome.COMPLETED;
	private final List<Boolean> finalFlags = new ArrayList<>();
	private AiJobRunner runner;

	@BeforeEach
	void setUp() {
		repository.deleteAll();
		AiJobProcessor processor = new AiJobProcessor() {
			@Override
			public AiJobType type() {
				return AiJobType.SESSION_EVALUATION;
			}

			@Override
			public Outcome process(UUID subjectId, boolean finalAttempt) {
				processed.add(subjectId);
				finalFlags.add(finalAttempt);
				return behaviour.apply(subjectId);
			}
		};
		runner = new AiJobRunner(repository, properties, List.of(processor), transactionManager);
	}

	@Test
	void completedJobSucceedsAndIsNotRunAgain() {
		UUID subject = enqueue();

		assertThat(runner.runOnce()).isEqualTo(1);
		assertThat(runner.runOnce()).isZero();

		AiJob job = job(subject);
		assertThat(job.getStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
		assertThat(job.getAttempts()).isEqualTo(1);
		assertThat(job.getLockedBy()).isNull();
		assertThat(processed).containsExactly(subject);
	}

	@Test
	void transientFailureBacksOffThenFailsAfterMaxAttempts() {
		behaviour = subject -> Outcome.RETRY;
		UUID subject = enqueue();

		Instant before = Instant.now();
		runner.runOnce();
		AiJob afterFirst = job(subject);
		assertThat(afterFirst.getStatus()).isEqualTo(AiJobStatus.READY);
		assertThat(afterFirst.getAttempts()).isEqualTo(1);
		assertThat(Duration.between(before, afterFirst.getNextRunAt()).toSeconds()).isCloseTo(15, within(2L));
		assertThat(runner.runOnce()).as("not due yet").isZero();

		makeDue(subject);
		runner.runOnce();
		assertThat(Duration.between(Instant.now(), job(subject).getNextRunAt()).toSeconds()).isCloseTo(60, within(2L));

		makeDue(subject);
		runner.runOnce();
		AiJob last = job(subject);
		assertThat(last.getStatus()).isEqualTo(AiJobStatus.FAILED);
		assertThat(last.getAttempts()).isEqualTo(3);
		assertThat(last.getLastError()).isNotBlank();
		assertThat(finalFlags).containsExactly(false, false, true);
	}

	@Test
	void unexpectedExceptionsCountAsTransient() {
		behaviour = subject -> {
			throw new IllegalStateException("boom");
		};
		UUID subject = enqueue();

		runner.runOnce();

		assertThat(job(subject).getStatus()).isEqualTo(AiJobStatus.READY);
		assertThat(job(subject).getLastError()).contains("boom");
	}

	@Test
	void runningJobsAreNotClaimedTwiceButStaleOnesAreReleased() {
		UUID subject = enqueue();
		jdbc.update("update ai_job set status = 'RUNNING', locked_by = 'other', locked_at = now() where subject_id = ?",
			subject);

		assertThat(runner.runOnce()).as("held by another worker").isZero();

		jdbc.update("update ai_job set locked_at = now() - interval '11 minutes' where subject_id = ?", subject);
		assertThat(runner.runOnce()).as("stale lock released and reclaimed").isEqualTo(1);
		assertThat(job(subject).getStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
	}

	@Test
	void backoffUsesTheLastStepOnceExhausted() {
		assertThat(runner.backoff(1)).isEqualTo(Duration.ofSeconds(15));
		assertThat(runner.backoff(3)).isEqualTo(Duration.ofSeconds(300));
		assertThat(runner.backoff(9)).isEqualTo(Duration.ofSeconds(300));
	}

	@Test
	void manualRequeueResetsAttemptsUpToTheLimit() {
		behaviour = subject -> Outcome.RETRY;
		UUID subject = enqueue();
		runner.runOnce();

		assertThat(commandService.requeueManually(AiJobType.SESSION_EVALUATION, subject, 1)).isTrue();
		AiJob requeued = job(subject);
		assertThat(requeued.getAttempts()).isZero();
		assertThat(requeued.getManualRetries()).isEqualTo(1);
		assertThat(requeued.getStatus()).isEqualTo(AiJobStatus.READY);
		assertThat(commandService.requeueManually(AiJobType.SESSION_EVALUATION, subject, 1)).isFalse();
	}

	private UUID enqueue() {
		UUID subject = UUID.randomUUID();
		commandService.enqueue(AiJobType.SESSION_EVALUATION, subject);
		return subject;
	}

	private void makeDue(UUID subject) {
		jdbc.update("update ai_job set next_run_at = now() - interval '1 second' where subject_id = ?", subject);
	}

	private AiJob job(UUID subject) {
		return repository.findByDedupeKey(AiJob.dedupeKey(AiJobType.SESSION_EVALUATION, subject)).orElseThrow();
	}
}
