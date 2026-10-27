package com.opicer.api.evaluation.application;

import com.opicer.api.evaluation.domain.EvaluationMode;

/**
 * @param instructions stable system text (rubric, rules); identical across calls so providers can cache it
 * @param input the per-answer part (question, signals, references, transcript)
 * @param mode selects the output schema the provider enforces
 */
public record EvaluationPrompt(String instructions, String input, EvaluationMode mode) {

	public EvaluationPrompt(String instructions, String input) {
		this(instructions, input, EvaluationMode.FULL);
	}
}
