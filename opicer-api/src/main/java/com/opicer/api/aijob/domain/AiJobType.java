package com.opicer.api.aijob.domain;

/** Kinds of background AI work. Stored as text; new kinds (e.g. a speech-profile update) are added here. */
public enum AiJobType {
	/** Evaluate every answer of a practice session and build its report; subject = practice session id. */
	SESSION_EVALUATION,
	/** Evaluate every answer of a mock test and build its report and feedback; subject = mock test id. */
	MOCK_TEST_EVALUATION
}
