package com.opicer.api.mocktest.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SurveyCatalogTest {

	/** A complete survey with exactly 12 Part 4 selections. */
	static Map<String, List<String>> validSurvey() {
		Map<String, List<String>> survey = new HashMap<>();
		survey.put("OCCUPATION", List.of("occupation.business"));
		survey.put("STUDENT", List.of("student.no"));
		survey.put("RESIDENCE", List.of("residence.family"));
		survey.put("LEISURE", List.of("leisure.movies", "leisure.park", "leisure.tv", "leisure.shopping", "leisure.cafe"));
		survey.put("HOBBY", List.of("hobby.music", "hobby.reading"));
		survey.put("SPORT", List.of("sport.jogging", "sport.walking", "sport.swimming"));
		survey.put("TRAVEL", List.of("travel.domestic", "travel.overseas"));
		return survey;
	}

	@Test
	void aCompleteSurveyHasNoProblems() {
		assertThat(SurveyCatalog.problems(validSurvey())).isEmpty();
	}

	@Test
	void eachSingleChoicePartNeedsExactlyOne() {
		Map<String, List<String>> survey = validSurvey();
		survey.remove("OCCUPATION");
		survey.put("STUDENT", List.of("student.yes", "student.no"));

		assertThat(SurveyCatalog.problems(survey))
			.anyMatch(p -> p.startsWith("Part 1") && p.endsWith("하나를 골라 주세요."))
			.anyMatch(p -> p.startsWith("Part 2") && p.endsWith("하나를 골라 주세요."));
	}

	@Test
	void part4NeedsTwelveInTotalAndTheMinimumPerQuestion() {
		Map<String, List<String>> survey = validSurvey();
		survey.put("LEISURE", List.of("leisure.movies"));
		survey.put("SPORT", List.of());

		List<String> problems = SurveyCatalog.problems(survey);

		assertThat(problems)
			.anyMatch(p -> p.startsWith("4-1") && p.contains("2개 이상"))
			.anyMatch(p -> p.startsWith("4-3") && p.contains("1개 이상"))
			.anyMatch(p -> p.contains("12개 이상") && p.contains("지금 5개"));
	}

	@Test
	void unknownItemsDuplicatesAndPartsAreReported() {
		Map<String, List<String>> survey = validSurvey();
		survey.put("HOBBY", List.of("hobby.music", "hobby.music", "hobby.knitting"));
		survey.put("EXTRA", List.of("x"));

		assertThat(SurveyCatalog.problems(survey))
			.anyMatch(p -> p.contains("같은 항목을 두 번"))
			.anyMatch(p -> p.contains("hobby.knitting"))
			.anyMatch(p -> p.contains("EXTRA"));
	}

	@Test
	void selectionsMapToPracticeTopicsWithoutRepeats() {
		assertThat(SurveyCatalog.topicsOf(validSurvey())).containsExactly(
			"집/주거환경", "이웃/동네", "영화 감상", "TV 시청", "쇼핑", "음악 감상", "독서", "조깅/걷기", "수영",
			"국내 여행", "해외 여행");
	}

	@Test
	void everyTopicTheCatalogPointsToIsOneOfTheSeededTopics() {
		List<String> seeded = List.of("집/주거환경", "이웃/동네", "음악 감상", "영화 감상", "TV 시청", "독서", "요리",
			"쇼핑", "공연/콘서트 관람", "캠핑", "헬스/운동", "수영", "자전거 타기", "조깅/걷기", "국내 여행", "해외 여행", "출장");

		SurveyCatalog.PARTS.forEach(part -> part.items().forEach(item ->
			assertThat(seeded).as(item.id()).containsAll(item.topics())));
	}
}
