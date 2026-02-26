package com.opicer.api.practice.presentation;

import com.opicer.api.practice.application.PracticeAiService;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/practice")
public class PracticeAiController {

	private final PracticeAiService practiceAiService;

	public PracticeAiController(PracticeAiService practiceAiService) {
		this.practiceAiService = practiceAiService;
	}

	@PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<Map<String, String>> transcribe(
		@RequestPart("audio") MultipartFile audio,
		@RequestParam("questionText") String questionText
	) {
		String transcript = practiceAiService.transcribe(audio, questionText);
		return ApiResponse.ok("Transcription complete", Map.of("transcript", transcript));
	}

	@PostMapping("/analyze")
	public ApiResponse<Map<String, String>> analyze(@Valid @RequestBody AnalyzeRequest request) {
		String analysis = practiceAiService.analyze(
			request.topicId(),
			request.questionText(),
			request.transcript()
		);
		return ApiResponse.ok("Analysis complete", Map.of("analysis", analysis));
	}

	@PostMapping("/improve")
	public ApiResponse<Map<String, String>> improve(@Valid @RequestBody ImproveRequest request) {
		String improved = practiceAiService.improve(
			request.topicId(),
			request.questionText(),
			request.transcript()
		);
		return ApiResponse.ok("Improvement complete", Map.of("improved", improved));
	}

	record AnalyzeRequest(
		@NotNull java.util.UUID topicId,
		@NotBlank String questionText,
		@NotBlank String transcript
	) {}

	record ImproveRequest(
		@NotNull java.util.UUID topicId,
		@NotBlank String questionText,
		@NotBlank String transcript
	) {}
}
