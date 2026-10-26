package com.opicer.api.evaluation.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure, deterministic signal extraction from a transcript (and the recording length when known). */
public final class SpeechSignalsCalculator {

	static final int MATTR_WINDOW = 50;

	private static final Pattern WORD = Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)?");
	private static final Pattern SENTENCE_END = Pattern.compile("[.!?]+");
	private static final Set<String> FILLERS = Set.of("um", "uh", "er", "erm", "hmm", "uhm", "mm");

	private SpeechSignalsCalculator() {
	}

	public static SpeechSignals calculate(String transcript, Integer durationSeconds) {
		List<String> words = words(transcript);
		if (words.isEmpty()) {
			return SpeechSignals.EMPTY;
		}
		int wordCount = words.size();
		int sentenceCount = sentenceCount(transcript);
		double averageSentenceLength = round1((double) wordCount / sentenceCount);
		Double wpm = durationSeconds == null || durationSeconds <= 0
			? null
			: round1(wordCount / (durationSeconds / 60.0));
		int fillers = (int) words.stream().filter(FILLERS::contains).count();
		return new SpeechSignals(wordCount, sentenceCount, averageSentenceLength, wpm, round3(lexicalDiversity(words)), fillers);
	}

	static List<String> words(String transcript) {
		List<String> words = new ArrayList<>();
		if (transcript == null) {
			return words;
		}
		Matcher matcher = WORD.matcher(transcript);
		while (matcher.find()) {
			words.add(matcher.group().toLowerCase(Locale.ROOT));
		}
		return words;
	}

	/** Spoken transcripts often lack punctuation; an unpunctuated answer counts as one sentence. */
	private static int sentenceCount(String transcript) {
		int count = 0;
		for (String part : SENTENCE_END.split(transcript)) {
			if (WORD.matcher(part).find()) {
				count++;
			}
		}
		return Math.max(1, count);
	}

	/**
	 * Moving-average type-token ratio: TTR averaged over every 50-word window, which (unlike plain TTR)
	 * does not fall just because an answer is longer.
	 */
	static double lexicalDiversity(List<String> words) {
		if (words.size() < MATTR_WINDOW) {
			return (double) Set.copyOf(words).size() / words.size();
		}
		Map<String, Integer> window = new HashMap<>();
		for (int i = 0; i < MATTR_WINDOW; i++) {
			window.merge(words.get(i), 1, Integer::sum);
		}
		double sum = window.size();
		int windows = 1;
		for (int i = MATTR_WINDOW; i < words.size(); i++) {
			String leaving = words.get(i - MATTR_WINDOW);
			if (window.merge(leaving, -1, Integer::sum) == 0) {
				window.remove(leaving);
			}
			window.merge(words.get(i), 1, Integer::sum);
			sum += window.size();
			windows++;
		}
		return sum / windows / MATTR_WINDOW;
	}

	private static double round1(double value) {
		return Math.round(value * 10) / 10.0;
	}

	private static double round3(double value) {
		return Math.round(value * 1000) / 1000.0;
	}
}
