package com.opicer.api.prompt.presentation;

import com.opicer.api.prompt.application.PromptVersionService;
import com.opicer.api.prompt.domain.PromptUseCase;
import com.opicer.api.prompt.domain.PromptVersion;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/admin/prompts")
public class AdminPromptVersionController {

	private final PromptVersionService promptVersionService;

	public AdminPromptVersionController(PromptVersionService promptVersionService) {
		this.promptVersionService = promptVersionService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<PromptVersionResponse>>> list() {
		List<PromptVersionResponse> versions = promptVersionService.findAll().stream()
			.map(PromptVersionResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok("PROMPT_LIST_OK", versions));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<PromptVersionResponse>> create(@Valid @RequestBody PromptVersionRequest request) {
		PromptVersion version = promptVersionService.create(
			request.useCase(), request.version(), request.name(), request.template());
		return ResponseEntity.status(201)
			.body(ApiResponse.created("PROMPT_CREATED", PromptVersionResponse.from(version)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Object> update(@PathVariable UUID id,
		@Valid @RequestBody PromptVersionRequest request) {
		return promptVersionService.update(id, request.useCase(), request.version(),
				request.name(), request.template())
			.<ResponseEntity<Object>>map(v -> ResponseEntity.ok(
				ApiResponse.ok("PROMPT_UPDATED", PromptVersionResponse.from(v))))
			.orElseThrow(() -> new ApiException(
				ErrorCode.PROMPT_NOT_FOUND, "PromptVersion not found: " + id));
	}

	@PostMapping("/{id}/activate")
	public ResponseEntity<Object> activate(@PathVariable UUID id) {
		return promptVersionService.activate(id)
			.<ResponseEntity<Object>>map(v -> ResponseEntity.ok(
				ApiResponse.ok("PROMPT_ACTIVATED", PromptVersionResponse.from(v))))
			.orElseThrow(() -> new ApiException(
				ErrorCode.PROMPT_NOT_FOUND, "PromptVersion not found: " + id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Object> delete(@PathVariable UUID id) {
		if (promptVersionService.delete(id)) {
			return ResponseEntity.ok(ApiResponse.ok("PROMPT_DELETED", null));
		}
		throw new ApiException(ErrorCode.PROMPT_NOT_FOUND, "PromptVersion not found: " + id);
	}

	public record PromptVersionRequest(
		@NotNull PromptUseCase useCase,
		@Positive int version,
		@NotBlank String name,
		@NotBlank String template
	) {}

	public record PromptVersionResponse(
		UUID id,
		PromptUseCase useCase,
		int version,
		String name,
		String template,
		boolean active,
		Instant createdAt,
		Instant updatedAt
	) {
		static PromptVersionResponse from(PromptVersion v) {
			return new PromptVersionResponse(v.getId(), v.getUseCase(), v.getVersion(),
				v.getName(), v.getTemplate(), v.isActive(), v.getCreatedAt(), v.getUpdatedAt());
		}
	}
}
