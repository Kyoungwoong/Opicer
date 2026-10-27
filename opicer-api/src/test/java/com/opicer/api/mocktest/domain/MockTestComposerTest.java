package com.opicer.api.mocktest.domain;

import com.opicer.api.mocktest.domain.MockTestComposer.Candidate;
import com.opicer.api.mocktest.domain.MockTestComposer.Pool;
import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.opicer.api.question.domain.QuestionType.COMPARE_CONTRAST;
import static com.opicer.api.question.domain.QuestionType.DESCRIPTION;
import static com.opicer.api.question.domain.QuestionType.OPINION;
import static com.opicer.api.question.domain.QuestionType.PAST_EXPERIENCE;
import static com.opicer.api.question.domain.QuestionType.ROLE_PLAY;
import static org.assertj.core.api.Assertions.assertThat;

class MockTestComposerTest {

	private static final List<String> HOME_AND_NEIGHBORHOOD = List.of("집/주거환경", "이웃/동네");

	private final List<Candidate> bank = new ArrayList<>();

	private Pool fullPool() {
		add("자기소개", DESCRIPTION, OpicLevel.IM);
		add("자기소개", OPINION, OpicLevel.IH);
		for (String topic : List.of("음악 감상", "영화 감상", "집/주거환경", "이웃/동네", "요리", "물건 분실", "기기 고장")) {
			add(topic, COMPARE_CONTRAST, OpicLevel.IH);
			add(topic, PAST_EXPERIENCE, OpicLevel.IM);
			add(topic, DESCRIPTION, OpicLevel.IL);
		}
		for (String set : List.of("롤플레이: A", "롤플레이: B")) {
			add(set, ROLE_PLAY, OpicLevel.IM);
			add(set, ROLE_PLAY, OpicLevel.IM);
			add(set, PAST_EXPERIENCE, OpicLevel.IM);
		}
		add("고난도: 사회", OPINION, OpicLevel.IL);
		add("고난도: 사회", OPINION, OpicLevel.IH);
		add("고난도: 사회", COMPARE_CONTRAST, OpicLevel.AL);
		add("고난도: 사회", COMPARE_CONTRAST, OpicLevel.IL);
		return new Pool(bank, Set.of("물건 분실", "기기 고장"), Set.of("음악 감상", "영화 감상", "집/주거환경", "이웃/동네", "요리"));
	}

	@ParameterizedTest
	@CsvSource({"1,,IL", "2,SAME,IL", "3,,IM", "4,HARDER,IH", "5,,IH", "6,HARDER,AL", "1,EASIER,IL", "6,EASIER,IM"})
	void difficultyBandFollowsSelfAssessmentAndTheReSelection(int selfAssessment, DifficultyAdjustment adjustment,
		OpicLevel expected) {
		assertThat(MockTestComposer.band(selfAssessment, adjustment)).isEqualTo(expected);
	}

	@Test
	void partOneIsASelfIntroductionThenTwoSurveyCombos() {
		Pool pool = fullPool();

		List<Candidate> part1 = new MockTestComposer(new Random(7))
			.part1(pool, List.of("음악 감상", "영화 감상"), HOME_AND_NEIGHBORHOOD, OpicLevel.IM);

		assertThat(part1).hasSize(MockTestComposer.PART1_SIZE);
		assertThat(part1.get(0).topic()).isEqualTo("자기소개");
		assertThat(part1.get(0).type()).isEqualTo(DESCRIPTION);
		assertThat(part1.subList(1, 4)).extracting(Candidate::topic).containsOnly(part1.get(1).topic());
		assertThat(part1.subList(4, 7)).extracting(Candidate::topic).containsOnly(part1.get(4).topic());
		assertThat(Set.of(part1.get(1).topic(), part1.get(4).topic())).containsExactlyInAnyOrder("음악 감상", "영화 감상");
		assertThat(part1.subList(1, 4)).extracting(Candidate::type).containsExactly(DESCRIPTION, PAST_EXPERIENCE, COMPARE_CONTRAST);
		assertDistinct(part1);
	}

	@Test
	void partTwoIsUnexpectedThenRolePlayThenAdvancedWithNothingRepeated() {
		Pool pool = fullPool();
		MockTestComposer composer = new MockTestComposer(new Random(7));
		List<String> survey = List.of("음악 감상", "영화 감상");
		List<Candidate> part1 = composer.part1(pool, survey, HOME_AND_NEIGHBORHOOD, OpicLevel.IM);

		List<Candidate> part2 = composer.part2(pool, survey, part1, OpicLevel.IH);

		assertThat(part2).hasSize(MockTestComposer.PART2_SIZE);
		String unexpected = part2.get(0).topic();
		assertThat(part2.subList(0, 3)).extracting(Candidate::topic).containsOnly(unexpected);
		assertThat(survey).doesNotContain(unexpected);
		assertThat(part1).extracting(Candidate::topic).doesNotContain(unexpected);
		assertThat(part2.get(3).topic()).startsWith("롤플레이:");
		assertThat(part2.subList(3, 6)).extracting(Candidate::order).isSorted();
		assertThat(part2.subList(3, 6)).extracting(Candidate::type).containsExactly(ROLE_PLAY, ROLE_PLAY, PAST_EXPERIENCE);
		assertThat(part2.subList(6, 8)).allMatch(c -> c.topic().startsWith("고난도:"));
		// Band IH prefers the IH and AL advanced questions over the IL ones.
		assertThat(part2.subList(6, 8)).extracting(c -> c.levels().iterator().next())
			.containsExactlyInAnyOrder(OpicLevel.IH, OpicLevel.AL);
		List<Candidate> all = new ArrayList<>(part1);
		all.addAll(part2);
		assertDistinct(all);
	}

	@Test
	void theSameSeedComposesTheSameTest() {
		Pool pool = fullPool();
		List<String> survey = List.of("음악 감상", "영화 감상", "요리");

		List<Candidate> first = new MockTestComposer(new Random(42))
			.part1(pool, survey, HOME_AND_NEIGHBORHOOD, OpicLevel.IM);
		List<Candidate> second = new MockTestComposer(new Random(42))
			.part1(pool, survey, HOME_AND_NEIGHBORHOOD, OpicLevel.IM);

		assertThat(first).extracting(Candidate::id).containsExactlyElementsOf(second.stream().map(Candidate::id).toList());
	}

	@Test
	void surveyTopicsWithoutQuestionsFallBackToTheResidenceTopics() {
		Pool pool = fullPool();

		List<Candidate> part1 = new MockTestComposer(new Random(1))
			.part1(pool, List.of("캠핑", "수영"), HOME_AND_NEIGHBORHOOD, OpicLevel.IM);

		assertThat(Set.of(part1.get(1).topic(), part1.get(4).topic())).containsExactlyInAnyOrder("집/주거환경", "이웃/동네");
	}

	@Test
	void aSmallBankFillsWhatItCanWithoutRepeatsOrErrors() {
		add("자기소개", DESCRIPTION, OpicLevel.IM);
		add("음악 감상", DESCRIPTION, OpicLevel.IM);
		add("음악 감상", PAST_EXPERIENCE, OpicLevel.IM);
		add("물건 분실", DESCRIPTION, OpicLevel.IM);
		Pool pool = new Pool(bank, Set.of("물건 분실"), Set.of("음악 감상"));
		MockTestComposer composer = new MockTestComposer(new Random(3));

		List<Candidate> part1 = composer.part1(pool, List.of("음악 감상"), HOME_AND_NEIGHBORHOOD, OpicLevel.IM);
		List<Candidate> part2 = composer.part2(pool, List.of("음악 감상"), part1, OpicLevel.IM);

		List<Candidate> all = new ArrayList<>(part1);
		all.addAll(part2);
		assertThat(all).hasSize(4);
		assertDistinct(all);
	}

	private void add(String topic, QuestionType type, OpicLevel level) {
		bank.add(new Candidate(UUID.randomUUID(), topic, type, Set.of(level), bank.size()));
	}

	private static void assertDistinct(List<Candidate> candidates) {
		assertThat(new HashSet<>(candidates.stream().map(Candidate::id).toList())).hasSize(candidates.size());
	}
}
