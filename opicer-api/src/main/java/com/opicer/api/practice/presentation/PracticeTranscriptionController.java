package com.opicer.api.practice.presentation;

import com.opicer.api.ai.application.AiTranscriptionService;
import com.opicer.api.shared.presentation.ApiResponse;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Speech-to-text for one recorded answer. The audio is transcribed and discarded, never stored. Feedback on the
 * transcript comes from the automatic session evaluation after submit.
 */
@RestController
@RequestMapping("/api/practice")
public class PracticeTranscriptionController {

	private final AiTranscriptionService transcriptionService;

	public PracticeTranscriptionController(AiTranscriptionService transcriptionService) {
		this.transcriptionService = transcriptionService;
	}

	/** @param questionText passed to the provider as a hint */
	@PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<Map<String, String>> transcribe(
		@RequestPart("audio") MultipartFile audio,
		@RequestParam("questionText") String questionText
	) {
		String transcript = transcriptionService.transcribe(audio, questionText);
		return ApiResponse.ok("Transcription complete", Map.of("transcript", transcript));
	}
}
