package com.opicer.api.aijob.infrastructure;

import com.opicer.api.aijob.domain.AiJob;
import com.opicer.api.aijob.domain.AiJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {

	/** Inserts a READY job unless one with the same dedupe key exists; returns rows inserted (0 or 1). */
	@Modifying
	@Query(value = """
		insert into ai_job (id, type, subject_id, dedupe_key, status, attempts, next_run_at, created_at, updated_at)
		values (:id, :type, :subjectId, :dedupeKey, 'READY', 0, now(), now(), now())
		on conflict (dedupe_key) do nothing
		""", nativeQuery = true)
	int insertIfAbsent(
		@Param("id") UUID id,
		@Param("type") String type,
		@Param("subjectId") UUID subjectId,
		@Param("dedupeKey") String dedupeKey
	);

	/**
	 * Due READY jobs, row-locked; concurrent workers skip each other's rows instead of waiting.
	 * Must run inside a transaction that then claims them.
	 */
	@Query(value = """
		select * from ai_job
		where status = 'READY' and next_run_at <= now()
		order by next_run_at
		limit :limit
		for update skip locked
		""", nativeQuery = true)
	List<AiJob> lockDueJobs(@Param("limit") int limit);

	/** Puts RUNNING jobs whose lock is older than {@code lockedBefore} back to READY (their worker died). */
	@Modifying
	@Query("""
		update AiJob j set j.status = :ready, j.lockedBy = null, j.lockedAt = null, j.updatedAt = :now
		where j.status = :running and j.lockedAt < :lockedBefore
		""")
	int releaseStale(@Param("lockedBefore") Instant lockedBefore, @Param("now") Instant now,
		@Param("ready") AiJobStatus ready, @Param("running") AiJobStatus running);

	Optional<AiJob> findByDedupeKey(String dedupeKey);

	long countByDedupeKey(String dedupeKey);

	@Modifying
	@Query("delete from AiJob j where j.dedupeKey = :dedupeKey")
	int deleteByDedupeKey(@Param("dedupeKey") String dedupeKey);
}
