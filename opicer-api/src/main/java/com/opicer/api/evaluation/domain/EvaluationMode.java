package com.opicer.api.evaluation.domain;

/**
 * How much an answer evaluation covers. Recorded in every stored evaluation's meta.
 */
public enum EvaluationMode {
	/** Competency levels, strengths, corrections, target script and expressions: the mock test report. */
	FULL,
	/** Corrections, target script and expressions only, with no levels: topic practice. A smaller, cheaper call. */
	COACH
}
