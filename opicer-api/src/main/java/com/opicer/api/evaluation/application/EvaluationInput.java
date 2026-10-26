package com.opicer.api.evaluation.application;

import com.opicer.api.evaluation.domain.SpeechSignals;
import com.opicer.api.goodanswer.application.ReferenceSample;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.List;

/**
 * Everything the prompt needs about one answer.
 *
 * @param questionType e.g. PAST_EXPERIENCE; may be null
 * @param structuralHint the question's suggested answer structure; may be null
 * @param keyExpressions expressions the question author attached; may be empty
 * @param references retrieved good-answer samples (expression ideas, not answer keys); may be empty
 */
public record EvaluationInput(
	String questionText,
	String questionType,
	String structuralHint,
	List<String> keyExpressions,
	String transcript,
	OpicLevel targetLevel,
	SpeechSignals signals,
	List<ReferenceSample> references
) {
	public EvaluationInput {
		keyExpressions = keyExpressions == null ? List.of() : List.copyOf(keyExpressions);
		references = references == null ? List.of() : List.copyOf(references);
	}
}
