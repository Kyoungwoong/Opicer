package com.opicer.api.evaluation.application;

import com.opicer.api.evaluation.application.EvaluationOutput.CorrectionItem;
import com.opicer.api.evaluation.application.EvaluationOutput.DimensionRating;
import com.opicer.api.evaluation.application.EvaluationOutput.KeyExpression;
import com.opicer.api.evaluation.domain.CompetencyDimension;
import com.opicer.api.evaluation.domain.CorrectionCategory;
import com.opicer.api.evaluation.domain.EvaluationMode;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.opicer.api.evaluation.application.EvaluationFixtures.TRANSCRIPT;
import static com.opicer.api.evaluation.application.EvaluationFixtures.validOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluationValidatorTest {

	@Test
	void coachingNeedsNoRatingsAndDropsAnyThatWereSent() {
		EvaluationOutput coached = new EvaluationOutput(null, null, validOutput().corrections(),
			validOutput().targetScript(), validOutput().keyExpressions());

		EvaluationOutput result = EvaluationValidator.validate(coached, TRANSCRIPT, EvaluationMode.COACH);
		EvaluationOutput extra = EvaluationValidator.validate(validOutput(), TRANSCRIPT, EvaluationMode.COACH);

		assertThat(result.dimensions()).isEmpty();
		assertThat(result.strengths()).isEmpty();
		assertThat(result.corrections()).hasSize(3);
		assertThat(extra.dimensions()).isEmpty();
		assertThat(extra.strengths()).isEmpty();
	}

	@Test
	void keepsAValidEvaluationAndCapsStrengths() {
		EvaluationOutput result = EvaluationValidator.validate(validOutput(), TRANSCRIPT);

		assertThat(result.dimensions()).extracting(DimensionRating::code).containsExactly(CompetencyDimension.values());
		assertThat(result.corrections()).hasSize(3);
		assertThat(result.strengths()).hasSize(EvaluationValidator.MAX_STRENGTHS);
		assertThat(result.keyExpressions()).hasSize(2);
	}

	@Test
	void missingOrDuplicatedCompetencyIsARetryableFailure() {
		EvaluationOutput valid = validOutput();
		EvaluationOutput missing = with(valid, valid.dimensions().subList(0, 3), valid.corrections());
		List<DimensionRating> twice = new ArrayList<>(valid.dimensions());
		twice.set(3, valid.dimensions().get(0));

		for (EvaluationOutput broken : List.of(missing, with(valid, twice, valid.corrections()))) {
			assertThatThrownBy(() -> EvaluationValidator.validate(broken, TRANSCRIPT))
				.isInstanceOfSatisfying(EvaluationFailure.class, e -> {
					assertThat(e.retryable()).isTrue();
					assertThat(e.reason()).isEqualTo("INVALID_OUTPUT");
				});
		}
	}

	@Test
	void dropsInventedQuotesButKeepsTheRating() {
		EvaluationOutput valid = validOutput();
		List<DimensionRating> dims = new ArrayList<>(valid.dimensions());
		dims.set(0, new DimensionRating(CompetencyDimension.TASK, OpicLevel.IM, "I visited Busan", "코멘트"));
		List<CorrectionItem> fixes = new ArrayList<>(valid.corrections());
		fixes.add(new CorrectionItem("I visited Busan", "I went to Busan", "지어낸 원문", CorrectionCategory.TENSE));

		EvaluationOutput result = EvaluationValidator.validate(with(valid, dims, fixes), TRANSCRIPT);

		assertThat(result.dimensions().get(0).evidence()).isEmpty();
		assertThat(result.dimensions().get(0).level()).isEqualTo(OpicLevel.IM);
		assertThat(result.corrections()).extracting(CorrectionItem::original).doesNotContain("I visited Busan");
	}

	@Test
	void quotesMatchIgnoringCaseSpacingAndCurlyQuotes() {
		EvaluationOutput valid = validOutput();
		List<CorrectionItem> fixes = List.of(
			new CorrectionItem("i  GO to jeju", "I went to Jeju", "", CorrectionCategory.TENSE));

		assertThat(EvaluationValidator.validate(with(valid, valid.dimensions(), fixes), "I go to Jeju").corrections())
			.hasSize(1);
		assertThat(EvaluationValidator.normalize("It’s “fine”")).isEqualTo("it's \"fine\"");
	}

	@Test
	void dropsNoOpDuplicateAndUncategorisedCorrectionsAndCapsAtEight() {
		EvaluationOutput valid = validOutput();
		List<CorrectionItem> fixes = new ArrayList<>();
		fixes.add(new CorrectionItem("I go to Jeju", "I go to Jeju", "바뀐 게 없음", CorrectionCategory.TENSE));
		fixes.add(new CorrectionItem("I go to Jeju", "I went to Jeju", "", CorrectionCategory.TENSE));
		fixes.add(new CorrectionItem("I go to Jeju", "I traveled to Jeju", "중복", CorrectionCategory.WORD_CHOICE));
		fixes.add(new CorrectionItem("really hot", "very hot", "", null));
		String[] spans = {"Last summer", "with my family", "We stayed", "in a hotel", "near the beach",
			"The weather", "was really hot", "every day", "a lot of seafood", "so delicious"};
		for (String span : spans) {
			fixes.add(new CorrectionItem(span, span + "!", "", CorrectionCategory.NATURALNESS));
		}

		List<CorrectionItem> kept = EvaluationValidator.validate(with(valid, valid.dimensions(), fixes), TRANSCRIPT)
			.corrections();

		assertThat(kept).hasSize(EvaluationValidator.MAX_CORRECTIONS);
		assertThat(kept.get(0).corrected()).isEqualTo("I went to Jeju");
		assertThat(kept).extracting(CorrectionItem::original).doesNotHaveDuplicates().doesNotContain("really hot");
	}

	@Test
	void targetScriptMustExistAndNotBalloon() {
		EvaluationOutput valid = validOutput();
		EvaluationOutput empty = new EvaluationOutput(valid.dimensions(), valid.strengths(), valid.corrections(), " ",
			valid.keyExpressions());
		EvaluationOutput huge = new EvaluationOutput(valid.dimensions(), valid.strengths(), valid.corrections(),
			"word ".repeat(400), valid.keyExpressions());

		for (EvaluationOutput broken : List.of(empty, huge)) {
			assertThatThrownBy(() -> EvaluationValidator.validate(broken, TRANSCRIPT))
				.isInstanceOfSatisfying(EvaluationFailure.class, e -> assertThat(e.retryable()).isTrue());
		}
	}

	@Test
	void keyExpressionsAreShortAndAtMostThree() {
		EvaluationOutput valid = validOutput();
		List<KeyExpression> expressions = List.of(
			new KeyExpression("one two three four five six seven eight nine", "너무 김"),
			new KeyExpression("Looking back", "돌이켜 보면"),
			new KeyExpression(" ", "빈 값"),
			new KeyExpression("To be honest", "솔직히"),
			new KeyExpression("On top of that", "게다가"),
			new KeyExpression("All in all", "결국"));

		List<KeyExpression> kept = EvaluationValidator.validate(new EvaluationOutput(valid.dimensions(),
			valid.strengths(), valid.corrections(), valid.targetScript(), expressions), TRANSCRIPT).keyExpressions();

		assertThat(kept).extracting(KeyExpression::expression)
			.containsExactly("Looking back", "To be honest", "On top of that");
	}

	@Test
	void nullOutputIsRetryable() {
		assertThatThrownBy(() -> EvaluationValidator.validate(null, TRANSCRIPT))
			.isInstanceOfSatisfying(EvaluationFailure.class, e -> assertThat(e.retryable()).isTrue());
	}

	private static EvaluationOutput with(EvaluationOutput base, List<DimensionRating> dims, List<CorrectionItem> fixes) {
		return new EvaluationOutput(dims, base.strengths(), fixes, base.targetScript(), base.keyExpressions());
	}
}
