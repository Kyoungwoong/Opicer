package com.opicer.api.mocktest.application;

import com.opicer.api.mocktest.infrastructure.SurveyTopicLinkBootstrapRunner;
import com.opicer.api.mocktest.infrastructure.SurveyTopicLinkRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Survey item → topic links on real Postgres: survey order, active topics only, renames and the seed. */
@SpringBootTest
class SurveyTopicLinkServiceTest {

	@Autowired
	private SurveyTopicLinkService service;

	@Autowired
	private SurveyTopicLinkRepository links;

	@Autowired
	private SurveyTopicLinkBootstrapRunner runner;

	@Autowired
	private TopicRepository topicRepository;

	@Test
	void chosenItemsGiveTheirActiveTopicsInSurveyOrderWithoutRepeats() {
		Topic first = topic("박물관", 1, true);
		Topic second = topic("게임", 2, true);
		Topic inactive = topic("체스", 3, false);
		service.replace("leisure.museum", List.of(second.getId()));
		service.replace("leisure.games", List.of(inactive.getId(), second.getId(), first.getId()));

		List<String> titles = service.topicsOf(Map.of("LEISURE", List.of("leisure.museum", "leisure.games")));

		assertThat(titles).containsExactly(second.getTitle(), first.getTitle());
	}

	@Test
	void aRenamedTopicKeepsItsLink() {
		Topic beach = topic("해변", 1, true);
		service.replace("leisure.beach", List.of(beach.getId()));
		String renamed = unique("바다");
		beach.update(renamed, "Beach", Topic.CATEGORY_SURVEY, 1, 1, List.of(), true);
		topicRepository.save(beach);

		assertThat(service.topicsOf(Map.of("LEISURE", List.of("leisure.beach")))).containsExactly(renamed);
	}

	@Test
	void theResidenceFallbackComesFromTheChosenResidenceOnly() {
		Topic dorm = topic("기숙사", 1, true);
		Topic museum = topic("박물관", 1, true);
		service.replace("residence.dormitory", List.of(dorm.getId()));
		service.replace("leisure.museum", List.of(museum.getId()));

		List<String> fallback = service.residenceTopics(Map.of(
			"RESIDENCE", List.of("residence.dormitory"),
			"LEISURE", List.of("leisure.museum")));

		assertThat(fallback).containsExactly(dorm.getTitle());
	}

	@Test
	void replacingChecksTheItemAndTheTopics() {
		Topic unexpected = topicRepository.save(new Topic(unique("정전"), "Blackout", Topic.CATEGORY_UNEXPECTED, 2, 1,
			List.of(), true));

		assertThatThrownBy(() -> service.replace("leisure.knitting", List.of()))
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SURVEY_ITEM_NOT_FOUND));
		assertThatThrownBy(() -> service.replace("leisure.sns", List.of(UUID.randomUUID())))
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
		assertThatThrownBy(() -> service.replace("leisure.sns", List.of(unexpected.getId())))
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
	}

	@Test
	void anEmptyTableIsSeededFromTheCatalogOnceAndThenLeftAlone() {
		Topic home = topicRepository.findByTitle("집/주거환경").orElseGet(() -> topicRepository.save(
			new Topic("집/주거환경", "Home", Topic.CATEGORY_SURVEY, 1, 1, List.of(), true)));
		links.deleteAll();

		runner.run(null);

		assertThat(service.topicsOf(Map.of("RESIDENCE", List.of("residence.family")))).contains(home.getTitle());
		service.replace("residence.family", List.of());
		runner.run(null);
		assertThat(service.topicsOf(Map.of("RESIDENCE", List.of("residence.family")))).isEmpty();
	}

	private Topic topic(String title, int order, boolean active) {
		return topicRepository.save(new Topic(unique(title), title, Topic.CATEGORY_SURVEY, 1, order, List.of(), active));
	}

	private static String unique(String title) {
		return title + " " + UUID.randomUUID().toString().substring(0, 8);
	}
}
