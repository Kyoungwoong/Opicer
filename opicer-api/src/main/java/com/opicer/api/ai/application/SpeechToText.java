package com.opicer.api.ai.application;

import org.springframework.core.io.InputStreamSource;

/**
 * Speech-to-text port. Callers depend only on this; the implementation is picked by
 * {@code opicer.ai.stt.provider} ({@code mock} | {@code openai}), so swapping providers is a config change.
 * The result is an estimate of what was said, not ground truth (CLAUDE.md §11).
 */
public interface SpeechToText {

	/**
	 * @param audio the recorded answer (a {@code MultipartFile} works directly)
	 * @param filename name sent to providers that infer the format from it, e.g. {@code audio.webm}
	 * @param hint optional context such as the question text; implementations may ignore it
	 * @return the transcript text
	 */
	String transcribe(InputStreamSource audio, String filename, String hint);

	/** Short provider id for logs and diagnostics. */
	String provider();
}
