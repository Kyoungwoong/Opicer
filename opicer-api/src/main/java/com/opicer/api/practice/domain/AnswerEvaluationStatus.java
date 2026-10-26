package com.opicer.api.practice.domain;

/** Evaluation progress of one answer. Null on answers recorded before automatic evaluation. */
public enum AnswerEvaluationStatus {
	PENDING,
	DONE,
	FAILED,
	/** Not evaluated: no transcript, or too short to judge. */
	SKIPPED
}
