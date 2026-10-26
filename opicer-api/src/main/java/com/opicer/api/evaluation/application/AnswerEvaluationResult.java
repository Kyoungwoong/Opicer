package com.opicer.api.evaluation.application;

import com.opicer.api.evaluation.domain.AnswerAssessment;
import com.opicer.api.evaluation.domain.SpeechSignals;
import com.opicer.api.shared.domain.OpicLevel;

/**
 * Outcome of evaluating one answer, ready to persist.
 *
 * @param skipped true when the answer was too short (or empty) to evaluate; the other fields are then null
 * @param evaluationJson the stored evaluation document ({@code schemaVersion} 1)
 * @param summaryKo Korean summary for {@code practice_answer.analysis}
 * @param assessment input for the session report
 */
public record AnswerEvaluationResult(
	boolean skipped,
	SpeechSignals signals,
	String evaluationJson,
	OpicLevel overallLevel,
	String summaryKo,
	String targetScript,
	AnswerAssessment assessment
) {
	static AnswerEvaluationResult skipped(SpeechSignals signals) {
		return new AnswerEvaluationResult(true, signals, null, null, null, null, null);
	}
}
