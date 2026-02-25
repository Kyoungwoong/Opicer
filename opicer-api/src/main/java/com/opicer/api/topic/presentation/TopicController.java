package com.opicer.api.topic.presentation;

import com.opicer.api.shared.presentation.ApiResponse;
import com.opicer.api.topic.application.TopicService;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.domain.TopicBadge;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

	private final TopicService topicService;

	public TopicController(TopicService topicService) {
		this.topicService = topicService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<TopicResponse>>> list() {
		List<TopicResponse> responses = topicService.findActive().stream()
			.map(TopicResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok("TOPIC_LIST_OK", responses));
	}

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
		Instant createdAt
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
				topic.getCreatedAt()
			);
		}
	}
}
