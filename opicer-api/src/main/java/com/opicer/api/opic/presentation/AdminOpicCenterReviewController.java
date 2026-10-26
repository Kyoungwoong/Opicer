package com.opicer.api.opic.presentation;

import com.opicer.api.opic.application.OpicCenterReviewCommandService;
import com.opicer.api.opic.application.OpicCenterReviewQueryService;
import com.opicer.api.opic.domain.OpicCenterReview;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

/** Admin CRUD for test center reviews. Reviews are entered by admins, not by end users. */
@RestController
@RequestMapping("/api/admin")
public class AdminOpicCenterReviewController {

	private final OpicCenterReviewQueryService reviewQueryService;
	private final OpicCenterReviewCommandService reviewCommandService;

	public AdminOpicCenterReviewController(
		OpicCenterReviewQueryService reviewQueryService,
		OpicCenterReviewCommandService reviewCommandService
	) {
		this.reviewQueryService = reviewQueryService;
		this.reviewCommandService = reviewCommandService;
	}

	@GetMapping("/opic-centers/{centerId}/reviews")
	public ResponseEntity<ApiResponse<List<AdminReviewResponse>>> list(@PathVariable UUID centerId) {
		return ResponseEntity.ok(ApiResponse.ok("OPIC_CENTER_REVIEW_LIST_OK",
			reviewQueryService.allReviews(centerId).stream().map(AdminReviewResponse::from).toList()));
	}

	@PostMapping("/opic-centers/{centerId}/reviews")
	public ResponseEntity<Object> create(@PathVariable UUID centerId, @Valid @RequestBody ReviewRequest request) {
		OpicCenterReview review = reviewCommandService.create(centerId, request.rating(), request.content(),
			request.sourceLabel(), request.visibleOrDefault());
		return ResponseEntity.status(201)
			.body(ApiResponse.created("OPIC_CENTER_REVIEW_CREATED", AdminReviewResponse.from(review)));
	}

	@PutMapping("/opic-center-reviews/{id}")
	public ResponseEntity<Object> update(@PathVariable UUID id, @Valid @RequestBody ReviewRequest request) {
		return reviewCommandService.update(id, request.rating(), request.content(), request.sourceLabel(),
				request.visibleOrDefault())
			.<ResponseEntity<Object>>map(r -> ResponseEntity.ok(
				ApiResponse.ok("OPIC_CENTER_REVIEW_UPDATED", AdminReviewResponse.from(r))))
			.orElseThrow(() -> new ApiException(
				ErrorCode.OPIC_CENTER_REVIEW_NOT_FOUND,
				"OPIc test center review not found: " + id));
	}

	@DeleteMapping("/opic-center-reviews/{id}")
	public ResponseEntity<Object> delete(@PathVariable UUID id) {
		if (reviewCommandService.delete(id)) {
			return ResponseEntity.ok(ApiResponse.ok("OPIC_CENTER_REVIEW_DELETED", null));
		}
		throw new ApiException(
			ErrorCode.OPIC_CENTER_REVIEW_NOT_FOUND,
			"OPIc test center review not found: " + id);
	}

	public record ReviewRequest(
		@NotNull @Min(1) @Max(5) Integer rating,
		@NotBlank @Size(max = 2000) String content,
		@Size(max = 100) String sourceLabel,
		Boolean visible
	) {
		boolean visibleOrDefault() {
			return visible == null || visible;
		}
	}

	public record AdminReviewResponse(
		UUID id,
		UUID centerId,
		int rating,
		String content,
		String sourceLabel,
		boolean visible,
		Instant createdAt,
		Instant updatedAt
	) {
		static AdminReviewResponse from(OpicCenterReview r) {
			return new AdminReviewResponse(r.getId(), r.getCenterId(), r.getRating(), r.getContent(),
				r.getSourceLabel(), r.isVisible(), r.getCreatedAt(), r.getUpdatedAt());
		}
	}
}
