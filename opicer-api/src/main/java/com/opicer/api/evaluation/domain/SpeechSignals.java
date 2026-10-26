package com.opicer.api.evaluation.domain;

/**
 * Measured facts about one answer's transcript. They are reference numbers, not a score: the transcript
 * is an STT estimate, and Whisper tends to drop fillers, so {@code fillerCount} undercounts.
 *
 * @param wordsPerMinute null when the recording length is unknown
 * @param lexicalDiversity MATTR over 50-word windows when there are at least 50 words, plain TTR otherwise
 */
public record SpeechSignals(
	int wordCount,
	int sentenceCount,
	double averageSentenceLength,
	Double wordsPerMinute,
	double lexicalDiversity,
	int fillerCount
) {
	public static final SpeechSignals EMPTY = new SpeechSignals(0, 0, 0, null, 0, 0);
}
