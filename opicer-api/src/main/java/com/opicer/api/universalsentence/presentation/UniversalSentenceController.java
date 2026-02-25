package com.opicer.api.universalsentence.presentation;

import com.opicer.api.shared.presentation.ApiResponse;
import com.opicer.api.universalsentence.application.UniversalSentenceService;
import com.opicer.api.universalsentence.domain.UniversalSentence;
import com.opicer.api.universalsentence.domain.UniversalSentenceType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/universal-sentences")
public class UniversalSentenceController {

	private final UniversalSentenceService universalSentenceService;

	public UniversalSentenceController(UniversalSentenceService universalSentenceService) {
		this.universalSentenceService = universalSentenceService;
	}

	@GetMapping("/random")
	public ResponseEntity<ApiResponse<List<UniversalSentenceResponse>>> random(
		@RequestParam(defaultValue = "4") int size
	) {
		List<UniversalSentenceResponse> responses = universalSentenceService.findRandom(size).stream()
			.map(UniversalSentenceResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok("UNIVERSAL_SENTENCE_RANDOM_OK", responses));
	}

	@GetMapping("/daily")
	public ResponseEntity<ApiResponse<List<UniversalSentenceResponse>>> daily() {
		List<UniversalSentenceResponse> responses = universalSentenceService.findDailySet().stream()
			.map(UniversalSentenceResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok("UNIVERSAL_SENTENCE_DAILY_OK", responses));
	}

	public record UniversalSentenceResponse(
		UUID id,
		UniversalSentenceType type,
		String title,
		String sentence,
		List<String> tags,
		Instant createdAt
	) {
		static UniversalSentenceResponse from(UniversalSentence s) {
			return new UniversalSentenceResponse(
				s.getId(),
				s.getType(),
				s.getTitle(),
				s.getSentence(),
				s.getTags(),
				s.getCreatedAt()
			);
		}
	}
}
