package com.opicer.api.evaluation.application;

import com.opicer.api.evaluation.application.EvaluationOutput.CorrectionItem;
import com.opicer.api.evaluation.application.EvaluationOutput.DimensionRating;
import com.opicer.api.evaluation.application.EvaluationOutput.KeyExpression;
import com.opicer.api.evaluation.domain.SessionReport;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.StringJoiner;

/**
 * Plain-text Korean summary of one evaluation, filled from a fixed template (no LLM prose). Stored in
 * {@code practice_answer.analysis} so screens that predate structured evaluation still show something useful.
 */
public final class EvaluationSummaryRenderer {

	static final int MAX_LISTED_CORRECTIONS = 3;

	private EvaluationSummaryRenderer() {
	}

	public static String render(EvaluationOutput evaluation, OpicLevel overallLevel) {
		StringJoiner text = new StringJoiner("\n");
		text.add("예상 레벨: " + overallLevel + " (" + SessionReport.DISCLAIMER + ")");
		text.add("");
		for (DimensionRating rating : evaluation.dimensions()) {
			text.add("• " + rating.code().labelKo() + " " + rating.level()
				+ (rating.commentKo().isEmpty() ? "" : " — " + rating.commentKo()));
		}
		if (!evaluation.strengths().isEmpty()) {
			text.add("");
			text.add("잘한 점");
			evaluation.strengths().forEach(s -> text.add("• " + s));
		}
		if (!evaluation.corrections().isEmpty()) {
			text.add("");
			text.add("고칠 점");
			evaluation.corrections().stream().limit(MAX_LISTED_CORRECTIONS).forEach(c -> text.add(correction(c)));
			int rest = evaluation.corrections().size() - MAX_LISTED_CORRECTIONS;
			if (rest > 0) {
				text.add("• 외 " + rest + "건");
			}
		}
		if (!evaluation.keyExpressions().isEmpty()) {
			text.add("");
			text.add("추천 표현");
			for (KeyExpression expression : evaluation.keyExpressions()) {
				text.add("• " + expression.expression()
					+ (expression.meaningKo().isEmpty() ? "" : " — " + expression.meaningKo()));
			}
		}
		return text.toString();
	}

	private static String correction(CorrectionItem c) {
		return "• [" + c.category().labelKo() + "] " + c.original() + " → " + c.corrected()
			+ (c.reasonKo().isEmpty() ? "" : " (" + c.reasonKo() + ")");
	}
}
