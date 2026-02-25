package com.opicer.api.practice.presentation;

import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.practice.application.TopicSelectionService;
import com.opicer.api.practice.domain.TopicSelection;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/practice/topic-selections")
public class TopicSelectionController {

	private final TopicSelectionService topicSelectionService;

	public TopicSelectionController(TopicSelectionService topicSelectionService) {
		this.topicSelectionService = topicSelectionService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<TopicSelectionResponse>> create(
		@Valid @RequestBody TopicSelectionRequest request,
		Authentication authentication
	) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
			return ResponseEntity.status(401).build();
		}
		TopicSelection selection = topicSelectionService.createSelection(principal.id(), request.topicId());
		return ResponseEntity.status(201).body(ApiResponse.created("TOPIC_SELECTION_CREATED",
			TopicSelectionResponse.from(selection)));
	}

	public record TopicSelectionRequest(
		@NotNull UUID topicId
	) {}

	public record TopicSelectionResponse(
		UUID id,
		UUID topicId,
		Instant selectedAt
	) {
		static TopicSelectionResponse from(TopicSelection selection) {
			return new TopicSelectionResponse(selection.getId(), selection.getTopicId(), selection.getSelectedAt());
		}
	}
}
