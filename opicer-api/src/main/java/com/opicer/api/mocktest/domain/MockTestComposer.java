package com.opicer.api.mocktest.domain;

import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * Picks the 15 questions of a mock test (docs/opic/mock-test-spec.md §5) from the question bank:
 * part 1 is a self-introduction and two survey-topic combos, part 2 an unexpected-topic combo, a role-play combo
 * and two advanced questions. Questions near the difficulty band come first; when the bank runs short, any unused
 * question fills the slot. Randomness comes from the injected {@link Random}, so tests are deterministic.
 */
public final class MockTestComposer {

	public static final int PART1_SIZE = 7;
	public static final int PART2_SIZE = 8;
	public static final int COMBO_SIZE = 3;
	public static final int ADVANCED_SIZE = 2;
	public static final String SELF_INTRO_TOPIC = "자기소개";
	public static final String ROLE_PLAY_PREFIX = "롤플레이:";
	public static final String ADVANCED_PREFIX = "고난도:";

	/** Combo order: describe, then tell a past experience, then compare or give an opinion. */
	private static final List<QuestionType> TYPE_ORDER = List.of(QuestionType.DESCRIPTION, QuestionType.PAST_EXPERIENCE,
		QuestionType.COMPARE_CONTRAST, QuestionType.OPINION, QuestionType.UNEXPECTED_SITUATION, QuestionType.ROLE_PLAY);

	private final Random random;

	public MockTestComposer(Random random) {
		this.random = random;
	}

	/** Self-assessment 1–2 → IL, 3–4 → IM, 5–6 → IH; the re-selection moves it one step, within IL..AL. */
	public static OpicLevel band(int selfAssessment, DifficultyAdjustment adjustment) {
		int base = selfAssessment <= 2 ? OpicLevel.IL.ordinal() : selfAssessment <= 4 ? OpicLevel.IM.ordinal() : OpicLevel.IH.ordinal();
		int shifted = base + (adjustment == null ? 0 : adjustment.shift());
		int clamped = Math.max(OpicLevel.IL.ordinal(), Math.min(OpicLevel.AL.ordinal(), shifted));
		return OpicLevel.values()[clamped];
	}

	/**
	 * Questions 1–7.
	 *
	 * @param fallbackTopics tried for the combos after the survey topics, before any other survey-category topic;
	 *     the topics the chosen residence links to (home and neighbourhood by default)
	 */
	public List<Candidate> part1(Pool pool, List<String> surveyTopics, List<String> fallbackTopics, OpicLevel band) {
		List<Candidate> picked = new ArrayList<>();
		Set<String> usedTopics = new HashSet<>();
		pickBest(pool.ofTopic(SELF_INTRO_TOPIC), band, picked, preferType(QuestionType.DESCRIPTION));
		usedTopics.add(SELF_INTRO_TOPIC);

		List<String> topics = new ArrayList<>(shuffled(surveyTopics));
		topics.addAll(shuffled(fallbackTopics));
		topics.addAll(shuffled(pool.surveyCategoryTopics()));
		int combos = 0;
		for (String topic : new LinkedHashSet<>(topics)) {
			if (combos == 2) {
				break;
			}
			if (usedTopics.contains(topic) || unused(pool.ofTopic(topic), picked).size() < 2) {
				continue;
			}
			picked.addAll(combo(pool, topic, band, picked));
			usedTopics.add(topic);
			combos++;
		}
		fill(pool, picked, PART1_SIZE, band);
		return picked;
	}

	/**
	 * Questions 8–15.
	 *
	 * @param earlier part-1 questions, so no question or combo topic repeats
	 */
	public List<Candidate> part2(Pool pool, List<String> surveyTopics, List<Candidate> earlier, OpicLevel band) {
		List<Candidate> picked = new ArrayList<>();
		List<Candidate> taken = new ArrayList<>(earlier);
		Set<String> usedTopics = new HashSet<>();
		earlier.forEach(c -> usedTopics.add(c.topic()));

		List<String> unexpected = new ArrayList<>(shuffled(pool.unexpectedTopics()));
		List<String> unchosen = pool.surveyCategoryTopics().stream().filter(t -> !surveyTopics.contains(t)).toList();
		unexpected.addAll(shuffled(unchosen));
		for (String topic : new LinkedHashSet<>(unexpected)) {
			if (!usedTopics.contains(topic) && unused(pool.ofTopic(topic), taken).size() >= 2) {
				List<Candidate> combo = combo(pool, topic, band, taken);
				picked.addAll(combo);
				taken.addAll(combo);
				break;
			}
		}

		for (String topic : shuffled(pool.topicsStartingWith(ROLE_PLAY_PREFIX))) {
			List<Candidate> set = unused(pool.ofTopic(topic), taken);
			if (!set.isEmpty()) {
				List<Candidate> combo = set.stream().sorted(Comparator.comparingInt(Candidate::order)).limit(COMBO_SIZE).toList();
				picked.addAll(combo);
				taken.addAll(combo);
				break;
			}
		}

		List<Candidate> advanced = pool.questions().stream()
			.filter(c -> c.topic().startsWith(ADVANCED_PREFIX))
			.toList();
		for (int i = 0; i < ADVANCED_SIZE; i++) {
			pickBest(unused(advanced, taken), band, taken, c -> 0).ifPresent(picked::add);
		}

		List<Candidate> all = new ArrayList<>(earlier);
		all.addAll(picked);
		int before = all.size();
		fill(pool, all, PART1_SIZE + PART2_SIZE, band);
		picked.addAll(all.subList(before, all.size()));
		return picked;
	}

	/** Up to three questions of one topic, best type mix first, then put in describe → experience → compare order. */
	private List<Candidate> combo(Pool pool, String topic, OpicLevel band, List<Candidate> taken) {
		List<Candidate> available = shuffled(unused(pool.ofTopic(topic), taken));
		List<Candidate> chosen = new ArrayList<>();
		for (QuestionType type : TYPE_ORDER) {
			if (chosen.size() == COMBO_SIZE) {
				break;
			}
			available.stream()
				.filter(c -> c.type() == type && !chosen.contains(c))
				.min(Comparator.comparingInt(c -> bandDistance(c, band)))
				.ifPresent(chosen::add);
		}
		available.stream()
			.filter(c -> !chosen.contains(c))
			.sorted(Comparator.comparingInt(c -> bandDistance(c, band)))
			.limit(COMBO_SIZE - chosen.size())
			.forEach(chosen::add);
		chosen.sort(Comparator.comparingInt(c -> typeRank(c.type())));
		return chosen;
	}

	/** Tops up with any unused question (never self-introduction), closest to the band first. */
	private void fill(Pool pool, List<Candidate> picked, int size, OpicLevel band) {
		List<Candidate> spare = shuffled(unused(pool.questions(), picked)).stream()
			.filter(c -> !SELF_INTRO_TOPIC.equals(c.topic()))
			.sorted(Comparator.comparingInt(c -> bandDistance(c, band)))
			.toList();
		for (Candidate candidate : spare) {
			if (picked.size() >= size) {
				return;
			}
			picked.add(candidate);
		}
	}

	private Optional<Candidate> pickBest(Collection<Candidate> candidates, OpicLevel band, List<Candidate> into,
		ToIntFunction<Candidate> preference) {
		Optional<Candidate> best = shuffled(unused(candidates, into)).stream()
			.min(Comparator.comparingInt(preference).thenComparingInt(c -> bandDistance(c, band)));
		best.ifPresent(into::add);
		return best;
	}

	private static ToIntFunction<Candidate> preferType(QuestionType type) {
		return c -> c.type() == type ? 0 : 1;
	}

	/** 0 when the question targets the band (or has no levels); otherwise the step distance to its nearest level. */
	private static int bandDistance(Candidate candidate, OpicLevel band) {
		if (candidate.levels().isEmpty()) {
			return 0;
		}
		return candidate.levels().stream().mapToInt(level -> Math.abs(level.ordinal() - band.ordinal())).min().orElse(0);
	}

	private static int typeRank(QuestionType type) {
		int rank = TYPE_ORDER.indexOf(type);
		return rank < 0 ? TYPE_ORDER.size() : rank;
	}

	private static List<Candidate> unused(Collection<Candidate> candidates, Collection<Candidate> taken) {
		Set<UUID> ids = new HashSet<>();
		taken.forEach(c -> ids.add(c.id()));
		return candidates.stream().filter(c -> !ids.contains(c.id())).toList();
	}

	private <T> List<T> shuffled(Collection<T> items) {
		List<T> copy = new ArrayList<>(items);
		Collections.shuffle(copy, random);
		return copy;
	}

	/**
	 * One active bank question.
	 *
	 * @param order insertion order within the bank; role-play sets are asked in this order
	 */
	public record Candidate(UUID id, String topic, QuestionType type, Set<OpicLevel> levels, int order) {}

	/**
	 * @param unexpectedTopics topics of the 돌발질문 category
	 * @param surveyCategoryTopics topics of the 설문조사 category
	 */
	public record Pool(List<Candidate> questions, Set<String> unexpectedTopics, Set<String> surveyCategoryTopics) {

		List<Candidate> ofTopic(String topic) {
			return questions.stream().filter(c -> c.topic().equals(topic)).toList();
		}

		List<String> topicsStartingWith(String prefix) {
			Map<String, Boolean> topics = new LinkedHashMap<>();
			questions.stream().map(Candidate::topic).filter(t -> t.startsWith(prefix)).forEach(t -> topics.put(t, true));
			return List.copyOf(topics.keySet());
		}
	}
}
