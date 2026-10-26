package com.opicer.api.evaluation.domain;

import com.opicer.api.evaluation.domain.AnswerAssessment.CorrectionSample;
import com.opicer.api.evaluation.domain.SessionReport.Confidence;
import com.opicer.api.evaluation.domain.SessionReport.IssueSummary;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static com.opicer.api.evaluation.domain.CompetencyDimension.ACCURACY;
import static com.opicer.api.evaluation.domain.CompetencyDimension.DISCOURSE;
import static com.opicer.api.evaluation.domain.CompetencyDimension.TASK;
import static com.opicer.api.evaluation.domain.CompetencyDimension.VOCABULARY;
import static com.opicer.api.shared.domain.OpicLevel.AL;
import static com.opicer.api.shared.domain.OpicLevel.IH;
import static com.opicer.api.shared.domain.OpicLevel.IL;
import static com.opicer.api.shared.domain.OpicLevel.IM;
import static com.opicer.api.shared.domain.OpicLevel.NL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LevelAggregatorTest {

	private final LevelAggregator aggregator = new LevelAggregator(LevelPolicy.DEFAULT);

	@Test
	void answerLevelIsTheLowerMedianOfCompetencies() {
		// sorted IM, IM, IH, AL → lower median is the 2nd → IM
		assertThat(aggregator.answerLevel(answer(levels(IH, IM, AL, IM), 100))).isEqualTo(IM);
		// sorted IL, IH, IH, AL → IH
		assertThat(aggregator.answerLevel(answer(levels(IH, IL, AL, IH), 100))).isEqualTo(IH);
	}

	@ParameterizedTest
	@CsvSource({"29, IL", "30, IM", "59, IM", "60, AL"})
	void shortAnswersAreCappedByLength(int words, OpicLevel expected) {
		assertThat(aggregator.answerLevel(answer(levels(AL, AL, AL, AL), words))).isEqualTo(expected);
	}

	@Test
	void capNeverRaisesALowLevel() {
		assertThat(aggregator.answerLevel(answer(levels(NL, NL, NL, NL), 10))).isEqualTo(NL);
	}

	@Test
	void sessionEstimateRangeAndGap() {
		SessionReport report = aggregator.aggregate(List.of(
			answer(levels(IM, IM, IM, IM), 80),
			answer(levels(IH, IH, IH, IH), 120),
			answer(levels(IL, IL, IL, IL), 80),
			answer(levels(IM, IH, IM, IM), 90)
		), IH);

		// answer levels: IM, IH, IL, IM → sorted IL, IM, IM, IH → lower median IM
		assertThat(report.estimatedLevel()).isEqualTo(IM);
		assertThat(report.lowestLevel()).isEqualTo(IL);
		assertThat(report.highestLevel()).isEqualTo(IH);
		assertThat(report.gapToTarget()).isEqualTo(1);
		assertThat(report.evaluatedAnswers()).isEqualTo(4);
		// spread 2 with 4 answers → MEDIUM
		assertThat(report.confidence()).isEqualTo(Confidence.MEDIUM);
		assertThat(report.dimensionLevels()).containsExactly(
			Map.entry(TASK, IM), Map.entry(DISCOURSE, IM), Map.entry(ACCURACY, IM), Map.entry(VOCABULARY, IM));
	}

	@ParameterizedTest
	@CsvSource({
		"4, 1, HIGH", "5, 0, HIGH", "4, 2, MEDIUM", "3, 3, MEDIUM",
		"2, 1, MEDIUM", "2, 2, LOW", "1, 0, LOW"
	})
	void confidenceBands(int answers, int spread, Confidence expected) {
		assertThat(LevelAggregator.confidence(answers, spread)).isEqualTo(expected);
	}

	@Test
	void topIssuesRankByAnswersThenCountThenCategory() {
		SessionReport report = aggregator.aggregate(List.of(
			answer(levels(IM, IM, IM, IM), 80,
				fix(CorrectionCategory.TENSE, "I go", "I went"),
				fix(CorrectionCategory.TENSE, "I eat", "I ate"),
				fix(CorrectionCategory.ARTICLE, "a apple", "an apple"),
				fix(CorrectionCategory.CONNECTOR, "and and", "so")),
			answer(levels(IM, IM, IM, IM), 80,
				fix(CorrectionCategory.ARTICLE, "the my", "my"),
				fix(CorrectionCategory.CONNECTOR, "and", "because")),
			answer(levels(IM, IM, IM, IM), 80,
				fix(CorrectionCategory.PREPOSITION, "in Monday", "on Monday"))
		), IM);

		// ARTICLE and CONNECTOR: 2 answers, 2 each → category order breaks the tie (ARTICLE first);
		// TENSE: 1 answer but 2 fixes beats PREPOSITION's 1 answer, 1 fix.
		assertThat(report.topIssues()).extracting(IssueSummary::category).containsExactly(
			CorrectionCategory.ARTICLE, CorrectionCategory.CONNECTOR, CorrectionCategory.TENSE);
		assertThat(report.topIssues().get(0).answerCount()).isEqualTo(2);
		assertThat(report.topIssues().get(2).totalCount()).isEqualTo(2);
		assertThat(report.topIssues().get(0).example().original()).isEqualTo("a apple");
	}

	@Test
	void tipsCoverTheTwoWeakestCompetenciesThenRecurringIssues() {
		SessionReport report = aggregator.aggregate(List.of(
			answer(levels(IH, IL, IM, AL), 120, fix(CorrectionCategory.TENSE, "I go", "I went"))
		), IH);

		assertThat(report.tips()).hasSize(3);
		assertThat(report.tips().get(0)).startsWith("담화 구성 → IM: ");
		assertThat(report.tips().get(1)).startsWith("문법·시제 → IH: ");
		assertThat(report.tips().get(2)).startsWith("시제: ");
		assertThat(report.confidence()).isEqualTo(Confidence.LOW);
	}

	@Test
	void allAlCompetenciesProduceNoLevelTips() {
		SessionReport report = aggregator.aggregate(List.of(answer(levels(AL, AL, AL, AL), 200)), AL);

		assertThat(report.tips()).isEmpty();
		assertThat(report.gapToTarget()).isZero();
	}

	@Test
	void rejectsEmptyInput() {
		assertThatThrownBy(() -> aggregator.aggregate(List.of(), IM)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> aggregator.answerLevel(answer(Map.of(), 100)))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void everyCompetencyDescribesEveryLevel() {
		for (CompetencyDimension dimension : CompetencyDimension.values()) {
			for (OpicLevel level : OpicLevel.values()) {
				assertThat(dimension.descriptor(level)).as(dimension + "/" + level).isNotBlank();
			}
		}
	}

	private static Map<CompetencyDimension, OpicLevel> levels(OpicLevel task, OpicLevel discourse,
		OpicLevel accuracy, OpicLevel vocabulary) {
		Map<CompetencyDimension, OpicLevel> map = new EnumMap<>(CompetencyDimension.class);
		map.put(TASK, task);
		map.put(DISCOURSE, discourse);
		map.put(ACCURACY, accuracy);
		map.put(VOCABULARY, vocabulary);
		return map;
	}

	private static AnswerAssessment answer(Map<CompetencyDimension, OpicLevel> levels, int words,
		CorrectionSample... corrections) {
		return new AnswerAssessment(levels, List.of(corrections), words);
	}

	private static CorrectionSample fix(CorrectionCategory category, String original, String corrected) {
		return new CorrectionSample(category, original, corrected);
	}
}
