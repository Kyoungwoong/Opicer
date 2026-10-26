package com.opicer.api.ai.infrastructure;

import com.opicer.api.evaluation.domain.SpeechSignalsCalculator;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamSource;
import static org.assertj.core.api.Assertions.assertThat;

class MockSpeechToTextTest {

	private static final InputStreamSource AUDIO = () -> new ByteArrayInputStream(new byte[] {1, 2, 3});

	private final MockSpeechToText stt = new MockSpeechToText();

	@Test
	void sameQuestionAlwaysGetsTheSameTranscript() {
		String question = "Tell me about your house.";

		assertThat(stt.transcribe(AUDIO, "audio.webm", question))
			.isEqualTo(stt.transcribe(AUDIO, "other.webm", question))
			.isNotBlank();
		assertThat(MockSpeechToText.indexFor(question)).isEqualTo(MockSpeechToText.indexFor(question));
	}

	@Test
	void differentQuestionsSpreadAcrossTheBank() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 60; i++) {
			seen.add(stt.transcribe(AUDIO, "audio.webm", "Question number " + i));
		}
		assertThat(seen).hasSizeGreaterThanOrEqualTo(MockSpeechToText.TRANSCRIPTS.size() - 1);
	}

	@Test
	void fallsBackToTheFilenameWithoutAHint() {
		assertThat(stt.transcribe(AUDIO, "sample.webm", null))
			.isEqualTo(MockSpeechToText.TRANSCRIPTS.get(MockSpeechToText.indexFor("sample.webm")));
		assertThat(stt.transcribe(AUDIO, "sample.webm", "  "))
			.isEqualTo(stt.transcribe(AUDIO, "sample.webm", null));
	}

	@Test
	void bankCoversShortToParagraphLengthAnswers() {
		int shortest = Integer.MAX_VALUE;
		int longest = 0;
		for (String transcript : MockSpeechToText.TRANSCRIPTS) {
			int words = SpeechSignalsCalculator.calculate(transcript, null).wordCount();
			shortest = Math.min(shortest, words);
			longest = Math.max(longest, words);
		}
		// Below the IL length cap (30 words) up to paragraph-level answers, so evaluation sees the full range.
		assertThat(shortest).isLessThan(30);
		assertThat(longest).isGreaterThan(120);
		assertThat(stt.provider()).isEqualTo("mock");
	}
}
