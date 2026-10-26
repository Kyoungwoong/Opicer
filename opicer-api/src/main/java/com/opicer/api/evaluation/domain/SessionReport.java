package com.opicer.api.evaluation.domain;

import com.opicer.api.evaluation.domain.AnswerAssessment.CorrectionSample;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.List;
import java.util.Map;

/**
 * Session-level evaluation sheet, computed deterministically from the answer assessments.
 * Every level here is an estimate for practice, never an official OPIc score ({@link #DISCLAIMER}).
 *
 * @param gapToTarget target minus estimate in level steps; positive means the learner is below the target
 */
public record SessionReport(
	OpicLevel estimatedLevel,
	OpicLevel lowestLevel,
	OpicLevel highestLevel,
	Confidence confidence,
	int evaluatedAnswers,
	Map<CompetencyDimension, OpicLevel> dimensionLevels,
	OpicLevel targetLevel,
	int gapToTarget,
	List<IssueSummary> topIssues,
	List<String> tips
) {
	public static final String DISCLAIMER = "예상 레벨(참고용) · 공식 OPIc 점수가 아닙니다";

	public enum Confidence { HIGH, MEDIUM, LOW }

	/** A correction category that recurred across the session, with one example fix. */
	public record IssueSummary(CorrectionCategory category, int answerCount, int totalCount, CorrectionSample example) {}
}
