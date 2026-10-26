package com.opicer.api.ai.application;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Transcribes uploaded answer audio through whichever {@link SpeechToText} provider is configured
 * ({@code opicer.ai.stt.provider}). The single entry point for practice answers and good-answer uploads.
 */
@Service
public class AiTranscriptionService {

	private static final String AUDIO_FILENAME = "audio.webm";

	private final SpeechToText speechToText;

	public AiTranscriptionService(SpeechToText speechToText) {
		this.speechToText = speechToText;
	}

	public String transcribe(MultipartFile audio) {
		return transcribe(audio, null);
	}

	/** @param hint optional context (the question text); providers may ignore it */
	public String transcribe(MultipartFile audio, String hint) {
		return speechToText.transcribe(audio, AUDIO_FILENAME, hint);
	}
}
