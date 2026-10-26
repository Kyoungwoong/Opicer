package com.opicer.api.evaluation.domain;

/**
 * Kinds of admin-written evaluation guide. Each kind is injected into the evaluation instructions with the
 * {@link #purpose()} that tells the model which part of its output the guide shapes.
 */
public enum EvaluationGuideKind {
	SCORING_NOTES("채점 보충", "Extra notes for judging each competency's level. They refine, never replace, the rubric."),
	TIPS("코칭 팁", "Coaching advice to reflect in the corrections' reasonKo and in strengths."),
	EXPRESSIONS("추천 표현", "Expressions to prefer for keyExpressions and the targetScript when they fit the answer."),
	REPORT("코멘트 문체", "Style and length for every Korean text field: commentKo, reasonKo, strengths, meaningKo.");

	private final String labelKo;
	private final String purpose;

	EvaluationGuideKind(String labelKo, String purpose) {
		this.labelKo = labelKo;
		this.purpose = purpose;
	}

	public String labelKo() {
		return labelKo;
	}

	public String purpose() {
		return purpose;
	}
}
