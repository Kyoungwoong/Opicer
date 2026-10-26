package com.opicer.api.evaluation.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opicer.api.evaluation.domain.CompetencyDimension;
import com.opicer.api.evaluation.domain.CorrectionCategory;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.List;

/**
 * Structured evaluation of one answer, in the shape the model is constrained to produce
 * ({@code OpenAiAnswerEvaluationModel#SCHEMA}). Also the cleaned shape after validation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvaluationOutput(
	List<DimensionRating> dimensions,
	List<String> strengths,
	List<CorrectionItem> corrections,
	String targetScript,
	List<KeyExpression> keyExpressions
) {

	/** @param evidence a verbatim span of the transcript backing the rating; may be empty */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record DimensionRating(CompetencyDimension code, OpicLevel level, String evidence, String commentKo) {}

	/** @param original verbatim from the transcript */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record CorrectionItem(String original, String corrected, String reasonKo, CorrectionCategory category) {}

	/** An expression worth using at the target level, with its Korean meaning. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record KeyExpression(String expression, String meaningKo) {}
}
