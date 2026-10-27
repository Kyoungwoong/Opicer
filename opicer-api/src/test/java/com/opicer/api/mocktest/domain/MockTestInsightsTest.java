package com.opicer.api.mocktest.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.evaluation.domain.CorrectionCategory;
import com.opicer.api.mocktest.domain.MockTestInsights.Insights;
import com.opicer.api.mocktest.domain.MockTestInsights.Item;
import com.opicer.api.mocktest.domain.MockTestInsights.TopicLink;
import com.opicer.api.mocktest.domain.MockTestInsights.TypeDiagnosis;
import com.opicer.api.mocktest.domain.MockTestInsights.WordCount;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.opicer.api.evaluation.domain.CorrectionCategory.ARTICLE;
import static com.opicer.api.evaluation.domain.CorrectionCategory.CONNECTOR;
import static com.opicer.api.evaluation.domain.CorrectionCategory.TENSE;
import static org.assertj.core.api.Assertions.assertThat;

class MockTestInsightsTest {

	private static final String PLAIN = "I live in Seoul with my family. We have a small apartment near the river.";

	@Test
	void typesAreRankedByTheirLowerMedianLevelWithTheMostFrequentIssue() {
		List<Item> items = List.of(
			evaluated(1, "DESCRIPTION", OpicLevel.IH, ARTICLE),
			evaluated(2, "DESCRIPTION", OpicLevel.IM),
			evaluated(3, "DESCRIPTION", OpicLevel.AL),
			evaluated(4, "PAST_EXPERIENCE", OpicLevel.IM, TENSE, TENSE, ARTICLE),
			evaluated(5, "PAST_EXPERIENCE", OpicLevel.IL, TENSE),
			evaluated(6, "ROLE_PLAY", OpicLevel.IL, CONNECTOR),
			evaluated(7, "OPINION", OpicLevel.AL));

		Insights insights = MockTestInsights.compute(items, null, Map.of());

		assertThat(insights.questionTypes()).extracting(TypeDiagnosis::type)
			.containsExactly("OPINION", "DESCRIPTION", "PAST_EXPERIENCE", "ROLE_PLAY");
		TypeDiagnosis past = insights.questionTypes().get(2);
		assertThat(past.level()).isEqualTo(OpicLevel.IL);
		assertThat(past.answers()).isEqualTo(2);
		assertThat(past.topIssue()).isEqualTo(TENSE);
		assertThat(past.label()).isEqualTo("과거 경험");
		assertThat(insights.questionTypes().get(1).level()).isEqualTo(OpicLevel.IH);
		assertThat(insights.questionTypes().get(0).topIssue()).isNull();
		assertThat(insights.strongestTypes()).containsExactly("OPINION");
		assertThat(insights.weakestTypes()).containsExactly("PAST_EXPERIENCE", "ROLE_PLAY");
	}

	@Test
	void unevaluatedAnswersAreLeftOutAndOneLevelEverywhereHasNoExtremes() {
		List<Item> items = List.of(
			evaluated(1, "DESCRIPTION", OpicLevel.IM),
			evaluated(2, "PAST_EXPERIENCE", OpicLevel.IM),
			new Item(3, 1, "OPINION", PLAIN, 60, false, null, List.of()));

		Insights insights = MockTestInsights.compute(items, null, Map.of());

		assertThat(insights.questionTypes()).extracting(TypeDiagnosis::type).containsExactly("DESCRIPTION", "PAST_EXPERIENCE");
		assertThat(insights.strongestTypes()).isEmpty();
		assertThat(insights.weakestTypes()).isEmpty();
		assertThat(insights.recommendations()).isEmpty();
	}

	@Test
	void weakTypesPointToUpToThreePracticeTopicsEach() {
		List<Item> items = List.of(
			evaluated(1, "DESCRIPTION", OpicLevel.IH),
			evaluated(2, "COMPARE_CONTRAST", OpicLevel.IL),
			evaluated(3, "ROLE_PLAY", OpicLevel.IL));
		List<TopicLink> compare = List.of(link("음악 감상"), link("영화 감상"), link("요리"), link("집/주거환경"));

		Insights insights = MockTestInsights.compute(items, null, Map.of("COMPARE_CONTRAST", compare));

		// Role play has no practice topic, so only the comparison recommendation is left.
		assertThat(insights.recommendations()).hasSize(1);
		assertThat(insights.recommendations().get(0).label()).isEqualTo("비교·변화");
		assertThat(insights.recommendations().get(0).topics()).extracting(TopicLink::title)
			.containsExactly("음악 감상", "영화 감상", "요리");
	}

	@Test
	void aDominantConnectorAndRepeatedWordsAreReported() {
		String answer = "I like movies and I watch movies and my friends like movies and we eat popcorn and "
			+ "we talk about movies and it is fun. So I go there because it is close.";
		List<Item> items = List.of(answered(1, answer, 90), answered(2, answer, 90));

		Insights insights = MockTestInsights.compute(items, null, Map.of());

		assertThat(insights.habits().connectors()).first().isEqualTo(new WordCount("and", 10));
		assertThat(insights.habits().dominantConnector()).isEqualTo("and");
		assertThat(insights.habits().dominantConnectorPercent()).isEqualTo(71);
		assertThat(insights.habits().repeatedWords()).first().isEqualTo(new WordCount("movies", 8));
		assertThat(insights.habits().notes()).anyMatch(n -> n.contains("71%") && n.contains("'and'"))
			.anyMatch(n -> n.contains("'movies'를 8번"));
	}

	@Test
	void phrasesAreCountedOnceAndTooFewConnectorsAreNeverDominant() {
		List<Item> items = List.of(answered(1, "After that, I went home. For example, that was fun.", 60));

		Insights insights = MockTestInsights.compute(items, null, Map.of());

		assertThat(insights.habits().connectors())
			.containsExactlyInAnyOrder(new WordCount("after that", 1), new WordCount("for example", 1));
		assertThat(insights.habits().dominantConnector()).isNull();
		assertThat(insights.habits().dominantConnectorPercent()).isNull();
	}

	@Test
	void answersWithoutPunctuationStillCountAsSeparateSentences() {
		List<Item> items = List.of(answered(1, "I like music a lot", 60), answered(2, "I play the guitar every day", 60));

		Insights insights = MockTestInsights.compute(items, null, Map.of());

		assertThat(insights.habits().averageSentenceLength()).isEqualTo(5.5);
		assertThat(insights.habits().notes()).anyMatch(n -> n.contains("짧아요"));
	}

	@ParameterizedTest
	@CsvSource({
		"600, 10분만 썼어요",
		"1500, ",
		"2350, 거의 다 썼어요"
	})
	void examTimeIsJudgedAgainstFortyMinutes(int examSeconds, String expectedNote) {
		Insights insights = MockTestInsights.compute(List.of(answered(1, PLAIN, 60)), examSeconds, Map.of());

		assertThat(insights.time().usedSeconds()).isEqualTo(examSeconds);
		assertThat(insights.time().limitSeconds()).isEqualTo(2400);
		if (expectedNote == null) {
			assertThat(insights.time().notes()).isEmpty();
		} else {
			assertThat(insights.time().notes()).anyMatch(n -> n.contains(expectedNote));
		}
	}

	@Test
	void shortLongAndUnansweredItemsAreListedPerPart() {
		List<Item> items = new ArrayList<>();
		items.add(new Item(1, 1, "DESCRIPTION", PLAIN, 20, false, null, List.of()));
		items.add(new Item(2, 1, "DESCRIPTION", PLAIN, 200, false, null, List.of()));
		items.add(new Item(3, 2, "ROLE_PLAY", PLAIN, 90, false, null, List.of()));
		items.add(new Item(4, 2, "ROLE_PLAY", " ", null, false, null, List.of()));

		Insights insights = MockTestInsights.compute(items, null, Map.of());

		assertThat(insights.time().part1Seconds()).isEqualTo(220);
		assertThat(insights.time().part2Seconds()).isEqualTo(90);
		assertThat(insights.time().usedSeconds()).isEqualTo(310);
		assertThat(insights.time().shortAnswers()).containsExactly(1);
		assertThat(insights.time().longAnswers()).containsExactly(2);
		assertThat(insights.time().unanswered()).containsExactly(4);
		assertThat(insights.time().items()).hasSize(4);
		assertThat(insights.time().notes()).anyMatch(n -> n.contains("30초가 안 되는 답변이 1개"))
			.anyMatch(n -> n.contains("3분이 넘는 답변이 1개"))
			.anyMatch(n -> n.contains("답하지 못한 문항이 1개"));
	}

	@Test
	void nothingAnsweredStillProducesASerialisableDocument() throws Exception {
		Insights insights = MockTestInsights.compute(List.of(new Item(1, 1, "DESCRIPTION", null, null, false, null,
			List.of())), 30, Map.of());

		JsonNode doc = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(insights));

		assertThat(doc.path("schemaVersion").asInt()).isEqualTo(MockTestInsights.SCHEMA_VERSION);
		assertThat(doc.path("questionTypes").isEmpty()).isTrue();
		assertThat(doc.path("habits").path("notes").isEmpty()).isTrue();
		assertThat(doc.path("time").path("unanswered").get(0).asInt()).isEqualTo(1);
	}

	private static Item evaluated(int order, String type, OpicLevel level, CorrectionCategory... corrections) {
		return new Item(order, order <= 7 ? 1 : 2, type, PLAIN, 60, true, level, List.of(corrections));
	}

	private static Item answered(int order, String transcript, int seconds) {
		return new Item(order, 1, "DESCRIPTION", transcript, seconds, false, null, List.of());
	}

	private static TopicLink link(String title) {
		return new TopicLink(UUID.randomUUID(), title);
	}
}
