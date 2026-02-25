package com.opicer.api.universalsentence.application;

import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.universalsentence.domain.UniversalSentence;
import com.opicer.api.universalsentence.domain.UniversalSentenceType;
import com.opicer.api.universalsentence.infrastructure.UniversalSentenceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversalSentenceServiceTest {

	@Mock
	private UniversalSentenceRepository repository;

	private UniversalSentenceService service;

	@BeforeEach
	void setUp() {
		Clock fixedClock = Clock.fixed(Instant.parse("1970-01-02T00:00:00Z"), ZoneOffset.UTC);
		service = new UniversalSentenceService(repository, fixedClock);
	}

	@Test
	void dailySetReturnsOnePerType() {
		when(repository.findByActiveTrueOrderByCreatedAtAscIdAsc()).thenReturn(seedSentences());

		List<UniversalSentence> result = service.findDailySet();

		assertThat(result).hasSize(4);
		assertThat(result).extracting(UniversalSentence::getType)
			.containsExactly(UniversalSentenceType.values());
		assertThat(result).extracting(UniversalSentence::getTitle)
			.containsExactlyElementsOf(expectedTitles());
	}

	@Test
	void sameDayReturnsSameSet() {
		when(repository.findByActiveTrueOrderByCreatedAtAscIdAsc()).thenReturn(seedSentences());

		List<UniversalSentence> first = service.findDailySet();
		List<UniversalSentence> second = service.findDailySet();

		assertThat(first).extracting(UniversalSentence::getTitle)
			.containsExactlyElementsOf(second.stream().map(UniversalSentence::getTitle).toList());
	}

	@Test
	void missingTypeThrowsApiException() {
		when(repository.findByActiveTrueOrderByCreatedAtAscIdAsc()).thenReturn(List.of(
			new UniversalSentence(UniversalSentenceType.OPINION, "Opinion 1", "Sentence", List.of("opinion"), true)
		));

		assertThatThrownBy(service::findDailySet)
			.isInstanceOf(ApiException.class)
			.satisfies(ex -> {
				ApiException apiException = (ApiException) ex;
				assertThat(apiException.getErrorCode()).isEqualTo(ErrorCode.UNIVERSAL_SENTENCE_DAILY_NOT_READY);
			});
	}

	private List<UniversalSentence> seedSentences() {
		return List.of(
			new UniversalSentence(UniversalSentenceType.OPINION, "Opinion 1", "Sentence", List.of("opinion"), true),
			new UniversalSentence(UniversalSentenceType.OPINION, "Opinion 2", "Sentence", List.of("opinion"), true),
			new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "Past 1", "Sentence", List.of("past"), true),
			new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "Past 2", "Sentence", List.of("past"), true),
			new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "Compare 1", "Sentence", List.of("compare"), true),
			new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "Compare 2", "Sentence", List.of("compare"), true),
			new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "Unexpected 1", "Sentence", List.of("unexpected"), true),
			new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "Unexpected 2", "Sentence", List.of("unexpected"), true)
		);
	}

	private List<String> expectedTitles() {
		long dayIndex = 1L;
		return List.of(
			pickTitle(dayIndex, UniversalSentenceType.OPINION, List.of("Opinion 1", "Opinion 2")),
			pickTitle(dayIndex, UniversalSentenceType.PAST_EXPERIENCE, List.of("Past 1", "Past 2")),
			pickTitle(dayIndex, UniversalSentenceType.COMPARE_CONTRAST, List.of("Compare 1", "Compare 2")),
			pickTitle(dayIndex, UniversalSentenceType.UNEXPECTED_SITUATION, List.of("Unexpected 1", "Unexpected 2"))
		);
	}

	private String pickTitle(long dayIndex, UniversalSentenceType type, List<String> titles) {
		int index = Math.floorMod(java.util.Objects.hash(dayIndex, type.name()), titles.size());
		return titles.get(index);
	}
}
