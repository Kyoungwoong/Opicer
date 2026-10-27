package com.opicer.api.mocktest.presentation;

import com.opicer.api.mocktest.application.SurveyTopicLinkService;
import com.opicer.api.mocktest.application.SurveyTopicLinkService.ItemLinks;
import com.opicer.api.mocktest.application.SurveyTopicLinkService.PartLinks;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Which practice topics each Background Survey item points to, for the mock test's survey combos. */
@RestController
@RequestMapping("/api/admin/survey-links")
public class AdminSurveyLinkController {

	private final SurveyTopicLinkService service;

	public AdminSurveyLinkController(SurveyTopicLinkService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<PartLinks>>> list() {
		return ResponseEntity.ok(ApiResponse.ok("SURVEY_LINK_LIST_OK", service.overview()));
	}

	@PutMapping("/{itemId}")
	public ResponseEntity<ApiResponse<ItemLinks>> replace(@PathVariable String itemId,
		@Valid @RequestBody ReplaceRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("SURVEY_LINK_UPDATED", service.replace(itemId, request.topicIds())));
	}

	public record ReplaceRequest(@NotNull List<@NotNull UUID> topicIds) {}
}
