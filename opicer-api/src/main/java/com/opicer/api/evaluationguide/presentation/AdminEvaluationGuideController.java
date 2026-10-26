package com.opicer.api.evaluationguide.presentation;

import com.opicer.api.evaluation.domain.EvaluationGuideKind;
import com.opicer.api.evaluationguide.application.EvaluationGuideCommandService;
import com.opicer.api.evaluationguide.application.EvaluationGuideQueryService;
import com.opicer.api.evaluationguide.domain.EvaluationGuide;
import com.opicer.api.shared.presentation.ApiResponse;
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

/** Markdown guides the evaluator reads (ROLE_ADMIN via the /api/admin/** rule). */
@RestController
@RequestMapping("/api/admin/evaluation-guides")
public class AdminEvaluationGuideController {

	private final EvaluationGuideQueryService queryService;
	private final EvaluationGuideCommandService commandService;

	public AdminEvaluationGuideController(EvaluationGuideQueryService queryService,
		EvaluationGuideCommandService commandService) {
		this.queryService = queryService;
		this.commandService = commandService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<GuideResponse>>> list() {
		List<GuideResponse> guides = queryService.findAll().stream().map(GuideResponse::from).toList();
		return ResponseEntity.ok(ApiResponse.ok("EVALUATION_GUIDE_LIST_OK", guides));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<GuideResponse>> create(@Valid @RequestBody CreateRequest request) {
		EvaluationGuide guide = commandService.create(request.kind(), request.title(), request.contentMd());
		return ResponseEntity.status(201)
			.body(ApiResponse.created("EVALUATION_GUIDE_CREATED", GuideResponse.from(guide)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<GuideResponse>> update(@PathVariable UUID id,
		@Valid @RequestBody UpdateRequest request) {
		EvaluationGuide guide = commandService.update(id, request.title(), request.contentMd());
		return ResponseEntity.ok(ApiResponse.ok("EVALUATION_GUIDE_UPDATED", GuideResponse.from(guide)));
	}

	@PostMapping("/{id}/activate")
	public ResponseEntity<ApiResponse<GuideResponse>> activate(@PathVariable UUID id) {
		return ResponseEntity.ok(ApiResponse.ok("EVALUATION_GUIDE_ACTIVATED", GuideResponse.from(commandService.activate(id))));
	}

	@PostMapping("/{id}/deactivate")
	public ResponseEntity<ApiResponse<GuideResponse>> deactivate(@PathVariable UUID id) {
		return ResponseEntity.ok(ApiResponse.ok("EVALUATION_GUIDE_DEACTIVATED", GuideResponse.from(commandService.deactivate(id))));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
		commandService.delete(id);
		return ResponseEntity.ok(ApiResponse.ok("EVALUATION_GUIDE_DELETED", null));
	}

	public record CreateRequest(
		@NotNull EvaluationGuideKind kind,
		@NotBlank @Size(max = EvaluationGuide.MAX_TITLE_LENGTH) String title,
		@NotBlank @Size(max = EvaluationGuide.MAX_CONTENT_LENGTH) String contentMd
	) {
	}

	/** The kind of a version is fixed; only its text changes, and only while inactive. */
	public record UpdateRequest(
		@NotBlank @Size(max = EvaluationGuide.MAX_TITLE_LENGTH) String title,
		@NotBlank @Size(max = EvaluationGuide.MAX_CONTENT_LENGTH) String contentMd
	) {
	}

	public record GuideResponse(
		UUID id,
		EvaluationGuideKind kind,
		String kindLabel,
		int version,
		String title,
		String contentMd,
		boolean active,
		Instant createdAt,
		Instant updatedAt
	) {
		static GuideResponse from(EvaluationGuide g) {
			return new GuideResponse(g.getId(), g.getKind(), g.getKind().labelKo(), g.getVersion(), g.getTitle(),
				g.getContentMd(), g.isActive(), g.getCreatedAt(), g.getUpdatedAt());
		}
	}
}
