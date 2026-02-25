package com.opicer.api.topic.presentation;

import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.presentation.ApiResponse;
import com.opicer.api.topic.application.TopicService;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.domain.TopicBadge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/admin/topics")
public class AdminTopicController {

	private final TopicService topicService;

	public AdminTopicController(TopicService topicService) {
		this.topicService = topicService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<TopicResponse>>> list() {
		List<TopicResponse> responses = topicService.findAll().stream()
			.map(TopicResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok("TOPIC_LIST_OK", responses));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<TopicResponse>> create(@Valid @RequestBody TopicRequest request) {
		Topic created = topicService.create(
			request.title(),
			request.englishTitle(),
			request.category(),
			request.categoryOrder(),
			request.topicOrder(),
			request.badges().stream().map(TopicBadgeRequest::toDomain).toList(),
			request.active() != null ? request.active() : true
		);
		return ResponseEntity.status(201).body(ApiResponse.created("TOPIC_CREATED", TopicResponse.from(created)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Object> update(@PathVariable UUID id, @Valid @RequestBody TopicRequest request) {
		return topicService.update(
				id,
				request.title(),
				request.englishTitle(),
				request.category(),
				request.categoryOrder(),
				request.topicOrder(),
				request.badges().stream().map(TopicBadgeRequest::toDomain).toList(),
				request.active() != null ? request.active() : true
			)
			.<ResponseEntity<Object>>map(updated -> ResponseEntity.ok(
				ApiResponse.ok("TOPIC_UPDATED", TopicResponse.from(updated))))
			.orElseThrow(() -> new ApiException(ErrorCode.TOPIC_NOT_FOUND, "Topic not found: " + id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Object> delete(@PathVariable UUID id) {
		if (topicService.delete(id)) {
			return ResponseEntity.ok(ApiResponse.ok("TOPIC_DELETED", null));
		}
		throw new ApiException(ErrorCode.TOPIC_NOT_FOUND, "Topic not found: " + id);
	}

	public record TopicBadgeRequest(
		@NotBlank String label,
		@Min(0) Integer count
	) {
		TopicBadge toDomain() {
			return new TopicBadge(label, count);
		}
	}

	public record TopicRequest(
		@NotBlank String title,
		@NotBlank String englishTitle,
		@NotBlank String category,
		@Min(0) int categoryOrder,
		@Min(0) int topicOrder,
		@NotNull List<@Valid TopicBadgeRequest> badges,
		Boolean active
	) {}

	public record TopicBadgeResponse(
		String label,
		Integer count
	) {
		static TopicBadgeResponse from(TopicBadge badge) {
			return new TopicBadgeResponse(badge.getLabel(), badge.getCount());
		}
	}

	public record TopicResponse(
		UUID id,
		String title,
		String englishTitle,
		String category,
		int categoryOrder,
		int topicOrder,
		List<TopicBadgeResponse> badges,
		boolean active,
		Instant createdAt,
		Instant updatedAt
	) {
		static TopicResponse from(Topic topic) {
			List<TopicBadgeResponse> badgeResponses = topic.getBadges().stream()
				.map(TopicBadgeResponse::from)
				.toList();
			return new TopicResponse(
				topic.getId(),
				topic.getTitle(),
				topic.getEnglishTitle(),
				topic.getCategory(),
				topic.getCategoryOrder(),
				topic.getTopicOrder(),
				badgeResponses,
				topic.isActive(),
				topic.getCreatedAt(),
				topic.getUpdatedAt()
			);
		}
	}
}
