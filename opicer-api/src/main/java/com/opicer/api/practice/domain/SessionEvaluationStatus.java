package com.opicer.api.practice.domain;

/**
 * Evaluation progress of a whole practice session. Null on sessions recorded before automatic evaluation
 * (or with nothing to evaluate).
 */
public enum SessionEvaluationStatus {
	PENDING,
	RUNNING,
	/** Every evaluable answer was evaluated. */
	DONE,
	/** Some answers were evaluated, some failed; the report covers the evaluated ones. */
	PARTIAL,
	FAILED
}
