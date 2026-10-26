package com.opicer.api.aijob.application;

import com.opicer.api.aijob.domain.AiJobType;
import java.util.UUID;

/** Does the work of one job type. Implementations must be idempotent: a job can run more than once. */
public interface AiJobProcessor {

	AiJobType type();

	/**
	 * @param finalAttempt true on the last automatic attempt; the processor must then settle every pending item
	 *                     (mark it failed) and return {@link Outcome#COMPLETED}
	 */
	Outcome process(UUID subjectId, boolean finalAttempt);

	enum Outcome {
		/** Nothing left to do for this job. */
		COMPLETED,
		/** Some work failed transiently; run the job again later. */
		RETRY
	}
}
