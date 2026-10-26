package com.opicer.api.aijob.application;

import com.opicer.api.aijob.domain.AiJob;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.aijob.infrastructure.AiJobRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiJobCommandService {

	private final AiJobRepository aiJobRepository;

	public AiJobCommandService(AiJobRepository aiJobRepository) {
		this.aiJobRepository = aiJobRepository;
	}

	/**
	 * Queues work for the subject; idempotent per {@code (type, subject)}. Joins the caller's transaction, so
	 * the job only becomes visible if the caller's own writes commit.
	 *
	 * @return true when a new job was queued
	 */
	@Transactional
	public boolean enqueue(AiJobType type, UUID subjectId) {
		return aiJobRepository.insertIfAbsent(UUID.randomUUID(), type.name(), subjectId,
			AiJob.dedupeKey(type, subjectId)) == 1;
	}

	/**
	 * Puts the subject's job back in the queue for a learner-requested retry, creating it if it is gone.
	 *
	 * @return false when the subject has used up {@code maxManualRetries}
	 */
	@Transactional
	public boolean requeueManually(AiJobType type, UUID subjectId, int maxManualRetries) {
		String key = AiJob.dedupeKey(type, subjectId);
		AiJob job = aiJobRepository.findByDedupeKey(key).orElse(null);
		if (job == null) {
			enqueue(type, subjectId);
			return true;
		}
		if (job.getManualRetries() >= maxManualRetries) {
			return false;
		}
		job.requeueManually(Instant.now());
		return true;
	}

	/** Removes the subject's job (physical deletion of derived data). */
	@Transactional
	public void delete(AiJobType type, UUID subjectId) {
		aiJobRepository.deleteByDedupeKey(AiJob.dedupeKey(type, subjectId));
	}
}
