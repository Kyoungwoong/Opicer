package com.opicer.api.aijob.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * A unit of background AI work (CLAUDE.md §12: DB job table, Kafka-extensible). Rows are inserted with
 * {@code on conflict (dedupe_key) do nothing}, so enqueueing the same work twice is a no-op; a worker
 * claims READY rows whose {@code next_run_at} has passed.
 */
@Entity
@Table(
	name = "ai_job",
	uniqueConstraints = @UniqueConstraint(name = "uk_ai_job_dedupe_key", columnNames = {"dedupe_key"}),
	indexes = @Index(name = "idx_ai_job_status_next_run", columnList = "status, next_run_at")
)
public class AiJob {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AiJobType type;

	@Column(name = "subject_id", nullable = false)
	private UUID subjectId;

	@Column(name = "dedupe_key", nullable = false, length = 100)
	private String dedupeKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private AiJobStatus status;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "next_run_at", nullable = false)
	private Instant nextRunAt;

	@Column(name = "locked_at")
	private Instant lockedAt;

	@Column(name = "locked_by", length = 64)
	private String lockedBy;

	@Column(name = "last_error", columnDefinition = "TEXT")
	private String lastError;

	/** Learner-requested retries; the default lets ddl-auto add the column to existing rows. */
	@Column(name = "manual_retries", nullable = false, columnDefinition = "integer default 0 not null")
	private int manualRetries;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AiJob() {
	}

	/** {@code type:subjectId} — one job per kind of work per subject. */
	public static String dedupeKey(AiJobType type, UUID subjectId) {
		return type.name() + ":" + subjectId;
	}

	public void claim(String node, Instant now) {
		this.status = AiJobStatus.RUNNING;
		this.lockedBy = node;
		this.lockedAt = now;
		this.attempts++;
		this.updatedAt = now;
	}

	public void succeed(Instant now) {
		this.status = AiJobStatus.SUCCEEDED;
		this.lastError = null;
		unlock(now);
	}

	public void retryAt(Instant nextRunAt, String error, Instant now) {
		this.status = AiJobStatus.READY;
		this.nextRunAt = nextRunAt;
		this.lastError = error;
		unlock(now);
	}

	public void fail(String error, Instant now) {
		this.status = AiJobStatus.FAILED;
		this.lastError = error;
		unlock(now);
	}

	/** A learner asked to try again: fresh automatic attempts, run as soon as possible. */
	public void requeueManually(Instant now) {
		this.status = AiJobStatus.READY;
		this.attempts = 0;
		this.nextRunAt = now;
		this.lastError = null;
		this.manualRetries++;
		unlock(now);
	}

	private void unlock(Instant now) {
		this.lockedBy = null;
		this.lockedAt = null;
		this.updatedAt = now;
	}

	public UUID getId() { return id; }
	public AiJobType getType() { return type; }
	public UUID getSubjectId() { return subjectId; }
	public String getDedupeKey() { return dedupeKey; }
	public AiJobStatus getStatus() { return status; }
	public int getAttempts() { return attempts; }
	public Instant getNextRunAt() { return nextRunAt; }
	public Instant getLockedAt() { return lockedAt; }
	public String getLockedBy() { return lockedBy; }
	public String getLastError() { return lastError; }
	public int getManualRetries() { return manualRetries; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
