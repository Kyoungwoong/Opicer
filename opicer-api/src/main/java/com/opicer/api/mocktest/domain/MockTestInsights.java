package com.opicer.api.mocktest.domain;

import com.opicer.api.evaluation.domain.CorrectionCategory;
import com.opicer.api.evaluation.domain.SpeechSignals;
import com.opicer.api.evaluation.domain.SpeechSignalsCalculator;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Feedback beyond the level report, computed from what the test already stored (transcripts, durations and the
 * per-answer evaluations) — no model call. Sentences come from fixed Korean templates.
 */
public final class MockTestInsights {

	public static final int SCHEMA_VERSION = 1;
	public static final int EXAM_LIMIT_SECONDS = 40 * 60;
	static final int SHORT_ANSWER_SECONDS = 30;
	static final int LONG_ANSWER_SECONDS = 180;
	static final int REPEATED_WORD_MIN = 4;
	static final int CONNECTOR_MIN_TOTAL = 8;
	static final int DOMINANT_CONNECTOR_PERCENT = 50;

	static final Map<String, String> TYPE_LABELS = Map.of(
		"DESCRIPTION", "묘사·일상",
		"PAST_EXPERIENCE", "과거 경험",
		"COMPARE_CONTRAST", "비교·변화",
		"OPINION", "의견",
		"ROLE_PLAY", "롤플레이",
		"UNEXPECTED_SITUATION", "문제 해결");

	/** Linking words and phrases worth counting; longer phrases are matched before their parts. */
	static final List<String> CONNECTORS = List.of("after that", "for example", "in addition", "and", "so", "but",
		"because", "then", "also", "however", "actually", "first", "finally");

	/** Function words and very common verbs that say nothing about a habit. */
	private static final Set<String> STOPWORDS = Set.of(
		"i", "me", "my", "mine", "we", "us", "our", "you", "your", "he", "him", "his", "she", "her", "it", "its",
		"they", "them", "their", "this", "that", "these", "those", "there", "here", "what", "which", "who", "when",
		"where", "how", "why", "a", "an", "the", "of", "to", "in", "on", "at", "for", "with", "from", "by", "about",
		"into", "or", "if", "as", "not", "no", "yes", "is", "am", "are", "was", "were", "be", "been", "being", "do",
		"does", "did", "have", "has", "had", "will", "would", "can", "could", "should", "just", "some", "any", "all",
		"one", "up", "out", "go", "went", "get", "got", "i'm", "it's", "don't", "there's", "that's", "lot", "lots",
		"time", "thing", "things", "very", "really");

	private static final Pattern WORD = Pattern.compile("[a-z]+(?:'[a-z]+)?");

	private MockTestInsights() {
	}

	/**
	 * @param examSeconds exam time the learner used; null falls back to the sum of answer durations
	 * @param topicsByType practice topics that have questions of each type, for the recommendations
	 */
	public static Insights compute(List<Item> items, Integer examSeconds, Map<String, List<TopicLink>> topicsByType) {
		List<TypeDiagnosis> types = typeDiagnosis(items);
		List<String> strongest = extreme(types, true);
		List<String> weakest = extreme(types, false);
		return new Insights(SCHEMA_VERSION, types, strongest, weakest, habits(items), time(items, examSeconds),
			recommendations(weakest, topicsByType));
	}

	private static List<TypeDiagnosis> typeDiagnosis(List<Item> items) {
		Map<String, List<Item>> byType = new LinkedHashMap<>();
		for (Item item : items) {
			if (item.evaluated() && item.level() != null && item.questionType() != null) {
				byType.computeIfAbsent(item.questionType(), t -> new ArrayList<>()).add(item);
			}
		}
		List<TypeDiagnosis> result = new ArrayList<>();
		byType.forEach((type, group) -> {
			List<OpicLevel> levels = group.stream().map(Item::level).sorted().toList();
			Map<CorrectionCategory, Long> issues = group.stream().flatMap(i -> i.corrections().stream())
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
			CorrectionCategory topIssue = issues.entrySet().stream()
				.max(Map.Entry.<CorrectionCategory, Long>comparingByValue().thenComparing(e -> -e.getKey().ordinal()))
				.map(Map.Entry::getKey)
				.orElse(null);
			result.add(new TypeDiagnosis(type, label(type), group.size(), levels.get((levels.size() - 1) / 2), topIssue));
		});
		result.sort(Comparator.comparing(TypeDiagnosis::level).reversed().thenComparing(TypeDiagnosis::answers,
			Comparator.reverseOrder()));
		return result;
	}

	/** Up to two types at the top (or bottom) level; none when every type sits at the same level. */
	private static List<String> extreme(List<TypeDiagnosis> types, boolean strongest) {
		Set<OpicLevel> levels = types.stream().map(TypeDiagnosis::level).collect(Collectors.toSet());
		if (levels.size() < 2) {
			return List.of();
		}
		OpicLevel target = strongest
			? types.get(0).level()
			: types.get(types.size() - 1).level();
		return types.stream().filter(t -> t.level() == target).map(TypeDiagnosis::type).limit(2).toList();
	}

	private static Habits habits(List<Item> items) {
		List<String> answered = items.stream().map(Item::transcript).filter(t -> t != null && !t.isBlank()).toList();
		String all = String.join(" ", answered).toLowerCase(Locale.ROOT);

		Map<String, Integer> connectorCounts = new LinkedHashMap<>();
		String remaining = " " + all.replaceAll("[^a-z' ]", " ") + " ";
		for (String connector : CONNECTORS) {
			Matcher matcher = Pattern.compile(" " + Pattern.quote(connector) + "(?= )").matcher(remaining);
			int count = 0;
			while (matcher.find()) {
				count++;
			}
			if (count > 0) {
				connectorCounts.put(connector, count);
				// Blank the phrase out so "after that" is not counted again as "that".
				remaining = matcher.replaceAll(" ");
			}
		}
		Set<String> connectorWords = new HashSet<>();
		CONNECTORS.forEach(c -> connectorWords.addAll(List.of(c.split(" "))));

		Map<String, Integer> wordCounts = new HashMap<>();
		Matcher words = WORD.matcher(all);
		while (words.find()) {
			String word = words.group();
			if (word.length() > 2 && !STOPWORDS.contains(word) && !connectorWords.contains(word)) {
				wordCounts.merge(word, 1, Integer::sum);
			}
		}
		List<WordCount> repeated = wordCounts.entrySet().stream()
			.filter(e -> e.getValue() >= REPEATED_WORD_MIN)
			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
			.limit(5)
			.map(e -> new WordCount(e.getKey(), e.getValue()))
			.toList();
		List<WordCount> connectors = connectorCounts.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
			.map(e -> new WordCount(e.getKey(), e.getValue()))
			.toList();

		int total = connectors.stream().mapToInt(WordCount::count).sum();
		String dominant = null;
		Integer dominantPercent = null;
		if (total >= CONNECTOR_MIN_TOTAL) {
			int percent = Math.round(100f * connectors.get(0).count() / total);
			if (percent >= DOMINANT_CONNECTOR_PERCENT) {
				dominant = connectors.get(0).word();
				dominantPercent = percent;
			}
		}

		// Each answer ends a sentence even when the transcript has no closing punctuation.
		SpeechSignals overall = SpeechSignalsCalculator.calculate(String.join(". ", answered), null);
		List<String> notes = new ArrayList<>();
		if (dominant != null) {
			notes.add("연결어의 " + dominantPercent + "%가 '" + dominant
				+ "'예요. because, so, after that처럼 다른 연결어를 섞어 보세요.");
		}
		if (!repeated.isEmpty()) {
			notes.add("'" + repeated.get(0).word() + "'를 " + repeated.get(0).count()
				+ "번 썼어요. 비슷한 뜻의 다른 표현으로 바꿔 보세요.");
		}
		if (overall.sentenceCount() > 0 && overall.averageSentenceLength() < 8) {
			notes.add("문장이 평균 " + oneDecimal(overall.averageSentenceLength())
				+ "단어로 짧아요. 이유나 예시를 이어 붙여 문장을 늘려 보세요.");
		} else if (overall.averageSentenceLength() > 25) {
			notes.add("문장이 평균 " + oneDecimal(overall.averageSentenceLength())
				+ "단어로 길어요. 한 문장에 한 가지만 담아 보세요.");
		}
		if (notes.isEmpty() && !answered.isEmpty()) {
			notes.add("눈에 띄게 반복된 습관은 없어요.");
		}
		return new Habits(repeated, connectors, dominant, dominantPercent, oneDecimal(overall.averageSentenceLength()),
			Math.round(overall.lexicalDiversity() * 100) / 100.0, notes);
	}

	private static Time time(List<Item> items, Integer examSeconds) {
		int part1 = 0;
		int part2 = 0;
		List<ItemTime> perItem = new ArrayList<>();
		List<Integer> shortAnswers = new ArrayList<>();
		List<Integer> longAnswers = new ArrayList<>();
		List<Integer> unanswered = new ArrayList<>();
		for (Item item : items) {
			int seconds = item.durationSeconds() == null ? 0 : item.durationSeconds();
			if (item.part() == 1) {
				part1 += seconds;
			} else {
				part2 += seconds;
			}
			perItem.add(new ItemTime(item.order(), item.part(), item.durationSeconds()));
			boolean answeredItem = item.transcript() != null && !item.transcript().isBlank();
			if (!answeredItem) {
				unanswered.add(item.order());
			} else if (seconds > 0 && seconds < SHORT_ANSWER_SECONDS) {
				shortAnswers.add(item.order());
			} else if (seconds > LONG_ANSWER_SECONDS) {
				longAnswers.add(item.order());
			}
		}
		int used = examSeconds != null ? examSeconds : part1 + part2;
		List<String> notes = new ArrayList<>();
		if (used < EXAM_LIMIT_SECONDS / 2) {
			notes.add("40분 중 " + Math.max(1, Math.round(used / 60f)) + "분만 썼어요. 답변을 더 길게 이어 가도 시간이 남아요.");
		} else if (used >= EXAM_LIMIT_SECONDS - 60) {
			notes.add("시간을 거의 다 썼어요. 한 문항에 너무 오래 머물지 않게 해 보세요.");
		}
		if (!shortAnswers.isEmpty()) {
			notes.add("30초가 안 되는 답변이 " + shortAnswers.size() + "개예요. 예시와 이유를 하나씩 더 붙여 보세요.");
		}
		if (!longAnswers.isEmpty()) {
			notes.add("3분이 넘는 답변이 " + longAnswers.size() + "개예요. 뒤 문항에 쓸 시간을 남겨 두세요.");
		}
		if (!unanswered.isEmpty()) {
			notes.add("답하지 못한 문항이 " + unanswered.size() + "개예요. 짧게라도 답하는 편이 좋아요.");
		}
		return new Time(used, EXAM_LIMIT_SECONDS, part1, part2, perItem, shortAnswers, longAnswers, unanswered, notes);
	}

	private static List<Recommendation> recommendations(List<String> weakest, Map<String, List<TopicLink>> topicsByType) {
		return weakest.stream()
			.map(type -> new Recommendation(type, label(type), topicsByType.getOrDefault(type, List.of()).stream()
				.limit(3).toList()))
			.filter(r -> !r.topics().isEmpty())
			.toList();
	}

	static String label(String type) {
		return TYPE_LABELS.getOrDefault(type, type);
	}

	private static double oneDecimal(double value) {
		return Math.round(value * 10) / 10.0;
	}

	/**
	 * One question of the test as the insights need it.
	 *
	 * @param evaluated true when the answer has a stored evaluation (so {@code level} and {@code corrections} count)
	 */
	public record Item(int order, int part, String questionType, String transcript, Integer durationSeconds,
		boolean evaluated, OpicLevel level, List<CorrectionCategory> corrections) {}

	public record TopicLink(UUID id, String title) {}

	/** Stored as {@code mock_test.insights}. */
	public record Insights(int schemaVersion, List<TypeDiagnosis> questionTypes, List<String> strongestTypes,
		List<String> weakestTypes, Habits habits, Time time, List<Recommendation> recommendations) {}

	/** @param level lower median of the answer levels of this type */
	public record TypeDiagnosis(String type, String label, int answers, OpicLevel level, CorrectionCategory topIssue) {}

	public record WordCount(String word, int count) {}

	public record Habits(List<WordCount> repeatedWords, List<WordCount> connectors, String dominantConnector,
		Integer dominantConnectorPercent, double averageSentenceLength, double lexicalDiversity, List<String> notes) {}

	public record ItemTime(int order, int part, Integer seconds) {}

	public record Time(int usedSeconds, int limitSeconds, int part1Seconds, int part2Seconds, List<ItemTime> items,
		List<Integer> shortAnswers, List<Integer> longAnswers, List<Integer> unanswered, List<String> notes) {}

	public record Recommendation(String type, String label, List<TopicLink> topics) {}
}
