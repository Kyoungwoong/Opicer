package com.opicer.api.evaluation.application;

/**
 * Port to the LLM that rates one answer. Callers depend only on this; the implementation is picked by
 * {@code opicer.ai.evaluation.provider}, so switching providers is a config change plus one class.
 * Implementations return the raw structured output; {@link EvaluationValidator} decides what to keep.
 */
public interface AnswerEvaluationModel {

	/** @throws EvaluationFailure when the provider declines, errors, or returns unusable output */
	EvaluationOutput evaluate(EvaluationPrompt prompt);

	String provider();

	String model();
}
