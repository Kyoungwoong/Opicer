package com.opicer.api.universalsentence.presentation;

import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.presentation.ApiResponse;
import com.opicer.api.universalsentence.application.UniversalSentenceService;
import com.opicer.api.universalsentence.domain.UniversalSentence;
import com.opicer.api.universalsentence.domain.UniversalSentenceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/universal-sentences")
public class AdminUniversalSentenceController {

	private final UniversalSentenceService universalSentenceService;

	public AdminUniversalSentenceController(UniversalSentenceService universalSentenceService) {
		this.universalSentenceService = universalSentenceService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<UniversalSentenceResponse>>> list() {
		List<UniversalSentenceResponse> responses = universalSentenceService.findAll().stream()
			.map(UniversalSentenceResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok("UNIVERSAL_SENTENCE_LIST_OK", responses));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<UniversalSentenceResponse>> create(
		@Valid @RequestBody UniversalSentenceRequest request
	) {
		UniversalSentence created = universalSentenceService.create(
			request.type(),
			request.title(),
			request.sentence(),
			request.tags(),
			request.active() != null ? request.active() : true
		);
		return ResponseEntity.status(201)
			.body(ApiResponse.created("UNIVERSAL_SENTENCE_CREATED", UniversalSentenceResponse.from(created)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Object> update(@PathVariable UUID id, @Valid @RequestBody UniversalSentenceRequest request) {
		return universalSentenceService.update(
				id,
				request.type(),
				request.title(),
				request.sentence(),
				request.tags(),
				request.active() != null ? request.active() : true
			)
			.<ResponseEntity<Object>>map(updated -> ResponseEntity.ok(
				ApiResponse.ok("UNIVERSAL_SENTENCE_UPDATED", UniversalSentenceResponse.from(updated))))
			.orElseThrow(() -> new ApiException(ErrorCode.UNIVERSAL_SENTENCE_NOT_FOUND,
				"Universal sentence not found: " + id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Object> delete(@PathVariable UUID id) {
		if (universalSentenceService.delete(id)) {
			return ResponseEntity.ok(ApiResponse.ok("UNIVERSAL_SENTENCE_DELETED", null));
		}
		throw new ApiException(ErrorCode.UNIVERSAL_SENTENCE_NOT_FOUND,
			"Universal sentence not found: " + id);
	}

	public record UniversalSentenceRequest(
		@NotNull UniversalSentenceType type,
		@NotBlank String title,
		@NotBlank String sentence,
		@Size(max = 8) List<@NotBlank String> tags,
		Boolean active
	) {}

	public record UniversalSentenceResponse(
		UUID id,
		UniversalSentenceType type,
		String title,
		String sentence,
		List<String> tags,
		boolean active,
		Instant createdAt,
		Instant updatedAt
	) {
		static UniversalSentenceResponse from(UniversalSentence s) {
			return new UniversalSentenceResponse(
				s.getId(),
				s.getType(),
				s.getTitle(),
				s.getSentence(),
				s.getTags(),
				s.isActive(),
				s.getCreatedAt(),
				s.getUpdatedAt()
			);
		}
	}
}
