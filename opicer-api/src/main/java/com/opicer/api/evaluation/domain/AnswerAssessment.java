package com.opicer.api.evaluation.domain;

import com.opicer.api.shared.domain.OpicLevel;
import java.util.List;
import java.util.Map;

/**
 * One evaluated answer, as the aggregator needs it: a level per competency, the kept corrections, and the
 * transcript's word count (for the length cap).
 */
public record AnswerAssessment(
	Map<CompetencyDimension, OpicLevel> dimensionLevels,
	List<CorrectionSample> corrections,
	int wordCount
) {
	public AnswerAssessment {
		dimensionLevels = Map.copyOf(dimensionLevels);
		corrections = List.copyOf(corrections);
	}

	/** A single original → corrected fix, used as the example for a recurring issue. */
	public record CorrectionSample(CorrectionCategory category, String original, String corrected) {}
}
