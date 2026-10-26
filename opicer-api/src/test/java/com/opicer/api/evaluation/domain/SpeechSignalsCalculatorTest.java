package com.opicer.api.evaluation.domain;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SpeechSignalsCalculatorTest {

	@Test
	void emptyOrNonEnglishTranscriptYieldsEmptySignals() {
		assertThat(SpeechSignalsCalculator.calculate(null, 30)).isEqualTo(SpeechSignals.EMPTY);
		assertThat(SpeechSignalsCalculator.calculate("   ", 30)).isEqualTo(SpeechSignals.EMPTY);
		assertThat(SpeechSignalsCalculator.calculate("... 123 !!", 30)).isEqualTo(SpeechSignals.EMPTY);
	}

	@Test
	void countsWordsSentencesAndPace() {
		SpeechSignals signals = SpeechSignalsCalculator.calculate(
			"I live in Seoul. It's a big city! Do you like it?", 30);

		// I live in Seoul / It's a big city / Do you like it → 4 + 4 + 4 words
		assertThat(signals.wordCount()).isEqualTo(12);
		assertThat(signals.sentenceCount()).isEqualTo(3);
		assertThat(signals.averageSentenceLength()).isEqualTo(4.0);
		assertThat(signals.wordsPerMinute()).isEqualTo(24.0);
	}

	@Test
	void unpunctuatedSpeechIsOneSentenceAndUnknownDurationHasNoPace() {
		SpeechSignals signals = SpeechSignalsCalculator.calculate("um i usually go to the park uh on weekends", null);

		assertThat(signals.sentenceCount()).isEqualTo(1);
		assertThat(signals.averageSentenceLength()).isEqualTo(signals.wordCount());
		assertThat(signals.wordsPerMinute()).isNull();
		assertThat(signals.fillerCount()).isEqualTo(2);
		assertThat(SpeechSignalsCalculator.calculate("hello there", 0).wordsPerMinute()).isNull();
	}

	@Test
	void shortAnswersUsePlainTypeTokenRatio() {
		// 4 distinct of 6 words
		assertThat(SpeechSignalsCalculator.calculate("the cat and the dog and", null).lexicalDiversity())
			.isCloseTo(4 / 6.0, within(1e-3));
	}

	@Test
	void mattrDoesNotPunishLongerAnswersLikePlainTtr() {
		// A 40-word vocabulary cycled: every 50-word window holds all 40 types, so MATTR stays 40/50 = 0.8
		// whether the answer is 150 or 300 words, while plain TTR would fall from 0.27 to 0.13.
		assertThat(SpeechSignalsCalculator.lexicalDiversity(cycle(150))).isCloseTo(0.8, within(1e-9));
		assertThat(SpeechSignalsCalculator.lexicalDiversity(cycle(300))).isCloseTo(0.8, within(1e-9));
	}

	private static List<String> cycle(int length) {
		List<String> words = new ArrayList<>();
		for (int i = 0; i < length; i++) {
			words.add(word(i % 40));
		}
		return words;
	}

	@Test
	void ignoresCaseAndKeepsContractions() {
		SpeechSignals signals = SpeechSignalsCalculator.calculate("It's IT it's it", null);

		assertThat(signals.wordCount()).isEqualTo(4);
		assertThat(signals.lexicalDiversity()).isEqualTo(0.5);
	}

	/** Distinct letter-only words: a, b, ..., z, ba, bb, ... */
	private static String word(int n) {
		StringBuilder sb = new StringBuilder();
		do {
			sb.append((char) ('a' + n % 26));
			n /= 26;
		} while (n > 0);
		return sb.toString();
	}
}
