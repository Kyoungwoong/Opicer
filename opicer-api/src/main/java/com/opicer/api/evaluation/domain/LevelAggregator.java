package com.opicer.api.evaluation.domain;

import com.opicer.api.evaluation.domain.AnswerAssessment.CorrectionSample;
import com.opicer.api.evaluation.domain.SessionReport.Confidence;
import com.opicer.api.evaluation.domain.SessionReport.IssueSummary;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns per-answer competency levels into answer levels and a session report — in code, not by the LLM, so
 * the same ratings always produce the same estimate. Levels are ordinal (NL &lt; IL &lt; IM &lt; IH &lt; AL) and
 * combined with the <em>lower</em> median, which keeps estimates on the conservative side.
 */
public final class LevelAggregator {

	static final int TOP_ISSUES = 3;
	static final int WEAK_DIMENSIONS = 2;

	private final LevelPolicy policy;

	public LevelAggregator(LevelPolicy policy) {
		this.policy = policy;
	}

	/** Lower median of the competency levels, then capped by answer length. */
	public OpicLevel answerLevel(AnswerAssessment answer) {
		if (answer.dimensionLevels().isEmpty()) {
			throw new IllegalArgumentException("answer has no competency levels");
		}
		OpicLevel level = lowerMedian(answer.dimensionLevels().values());
		if (answer.wordCount() < policy.ilMaxWords()) {
			return min(level, OpicLevel.IL);
		}
		if (answer.wordCount() < policy.imMaxWords()) {
			return min(level, OpicLevel.IM);
		}
		return level;
	}

	public SessionReport aggregate(List<AnswerAssessment> answers, OpicLevel targetLevel) {
		if (answers.isEmpty()) {
			throw new IllegalArgumentException("no evaluated answers to aggregate");
		}
		List<OpicLevel> answerLevels = answers.stream().map(this::answerLevel).toList();
		OpicLevel estimated = lowerMedian(answerLevels);
		OpicLevel lowest = Collections.min(answerLevels);
		OpicLevel highest = Collections.max(answerLevels);
		Map<CompetencyDimension, OpicLevel> dimensions = dimensionLevels(answers);
		List<IssueSummary> topIssues = topIssues(answers);
		return new SessionReport(
			estimated,
			lowest,
			highest,
			confidence(answerLevels.size(), highest.ordinal() - lowest.ordinal()),
			answers.size(),
			dimensions,
			targetLevel,
			targetLevel.ordinal() - estimated.ordinal(),
			topIssues,
			tips(dimensions, topIssues)
		);
	}

	static Confidence confidence(int answerCount, int spread) {
		if (answerCount >= 4 && spread <= 1) {
			return Confidence.HIGH;
		}
		if (answerCount >= 3 || (answerCount >= 2 && spread <= 1)) {
			return Confidence.MEDIUM;
		}
		return Confidence.LOW;
	}

	private static Map<CompetencyDimension, OpicLevel> dimensionLevels(List<AnswerAssessment> answers) {
		Map<CompetencyDimension, OpicLevel> result = new EnumMap<>(CompetencyDimension.class);
		for (CompetencyDimension dimension : CompetencyDimension.values()) {
			List<OpicLevel> levels = answers.stream()
				.map(a -> a.dimensionLevels().get(dimension))
				.filter(level -> level != null)
				.toList();
			if (!levels.isEmpty()) {
				result.put(dimension, lowerMedian(levels));
			}
		}
		return Collections.unmodifiableMap(result);
	}

	/** Ranked by how many answers show the issue, then how often, then category order. */
	private static List<IssueSummary> topIssues(List<AnswerAssessment> answers) {
		Map<CorrectionCategory, int[]> counts = new EnumMap<>(CorrectionCategory.class);
		Map<CorrectionCategory, CorrectionSample> examples = new EnumMap<>(CorrectionCategory.class);
		for (AnswerAssessment answer : answers) {
			Set<CorrectionCategory> seen = new LinkedHashSet<>();
			for (CorrectionSample correction : answer.corrections()) {
				int[] count = counts.computeIfAbsent(correction.category(), c -> new int[2]);
				count[1]++;
				if (seen.add(correction.category())) {
					count[0]++;
				}
				examples.putIfAbsent(correction.category(), correction);
			}
		}
		return counts.entrySet().stream()
			.sorted(Comparator
				.comparingInt((Map.Entry<CorrectionCategory, int[]> e) -> -e.getValue()[0])
				.thenComparingInt(e -> -e.getValue()[1])
				.thenComparing(Map.Entry::getKey))
			.limit(TOP_ISSUES)
			.map(e -> new IssueSummary(e.getKey(), e.getValue()[0], e.getValue()[1], examples.get(e.getKey())))
			.toList();
	}

	/** Next-level descriptors for the two weakest competencies, then one tip per recurring issue. */
	private static List<String> tips(Map<CompetencyDimension, OpicLevel> dimensions, List<IssueSummary> topIssues) {
		Set<String> tips = new LinkedHashSet<>();
		dimensions.entrySet().stream()
			.filter(e -> e.getValue() != OpicLevel.AL)
			.sorted(Map.Entry.<CompetencyDimension, OpicLevel>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
			.limit(WEAK_DIMENSIONS)
			.forEach(e -> {
				OpicLevel next = OpicLevel.values()[e.getValue().ordinal() + 1];
				tips.add(e.getKey().labelKo() + " → " + next + ": " + e.getKey().descriptor(next));
			});
		for (IssueSummary issue : topIssues) {
			tips.add(issue.category().labelKo() + ": " + issue.category().tipKo());
		}
		return List.copyOf(tips);
	}

	static OpicLevel lowerMedian(Collection<OpicLevel> levels) {
		List<OpicLevel> sorted = new ArrayList<>(levels);
		Collections.sort(sorted);
		return sorted.get((sorted.size() - 1) / 2);
	}

	private static OpicLevel min(OpicLevel a, OpicLevel b) {
		return a.compareTo(b) <= 0 ? a : b;
	}
}
