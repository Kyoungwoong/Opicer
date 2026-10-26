package com.opicer.api.evaluation.application;

import com.opicer.api.evaluation.application.EvaluationOutput.CorrectionItem;
import com.opicer.api.evaluation.application.EvaluationOutput.DimensionRating;
import com.opicer.api.evaluation.application.EvaluationOutput.KeyExpression;
import com.opicer.api.evaluation.domain.CompetencyDimension;
import com.opicer.api.evaluation.domain.CorrectionCategory;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.List;

/** A realistic model output for {@link #TRANSCRIPT} (an IM-ish past-experience answer). */
final class EvaluationFixtures {

	static final String TRANSCRIPT = "Last summer I go to Jeju island with my family. We stayed in a hotel near the beach. "
		+ "The weather was really hot, so we swim in the sea every day. I eat a lot of seafood and it was so delicious.";

	private EvaluationFixtures() {
	}

	static EvaluationOutput validOutput() {
		return new EvaluationOutput(
			List.of(
				new DimensionRating(CompetencyDimension.TASK, OpicLevel.IM, "We stayed in a hotel near the beach", "경험을 구체적으로 말했어요."),
				new DimensionRating(CompetencyDimension.DISCOURSE, OpicLevel.IM, "so we swim", "연결어로 문장을 이었어요."),
				new DimensionRating(CompetencyDimension.ACCURACY, OpicLevel.IL, "I go to Jeju", "과거 시제가 자주 틀려요."),
				new DimensionRating(CompetencyDimension.VOCABULARY, OpicLevel.IM, "really hot", "기본 어휘는 충분해요.")
			),
			List.of("여행 경험을 순서대로 말했어요.", "묘사가 자연스러워요.", "세 번째는 잘려야 해요"),
			List.of(
				new CorrectionItem("I go to Jeju", "I went to Jeju", "과거 경험은 과거 시제로 말해요.", CorrectionCategory.TENSE),
				new CorrectionItem("we swim in the sea", "we swam in the sea", "과거 시제", CorrectionCategory.TENSE),
				new CorrectionItem("I eat a lot of seafood", "I ate a lot of seafood", "과거 시제", CorrectionCategory.TENSE)
			),
			"Last summer, I went to Jeju Island with my family. We stayed at a hotel near the beach, and since it was "
				+ "really hot, we swam in the sea every day. I also ate a lot of seafood, which was delicious.",
			List.of(
				new KeyExpression("What I remember most is", "가장 기억에 남는 건"),
				new KeyExpression("It was the highlight of the trip", "여행의 하이라이트였어요")
			)
		);
	}
}
