package com.opicer.api.practice.presentation;

import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.practice.application.PracticeSessionCommandService;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/practice/sessions")
public class PracticeSessionController {

	private final PracticeSessionCommandService practiceSessionCommandService;

	public PracticeSessionController(PracticeSessionCommandService practiceSessionCommandService) {
		this.practiceSessionCommandService = practiceSessionCommandService;
	}

	@PostMapping("/submit")
	public ResponseEntity<ApiResponse<SubmitResponse>> submit(
		Authentication authentication,
		@Valid @RequestBody SubmitRequest request
	) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
			return ResponseEntity.status(401).build();
		}
		PracticeSessionCommandService.SubmitResult result = practiceSessionCommandService.submit(principal.id(), request.topicSelectionId());
		return ResponseEntity.ok(ApiResponse.ok("PRACTICE_SUBMITTED", new SubmitResponse(
			result.chargeId(),
			result.alreadyCharged(),
			result.deductedCredits()
		)));
	}

	public record SubmitRequest(@NotNull UUID topicSelectionId) {
	}

	public record SubmitResponse(
		UUID chargeId,
		boolean alreadyCharged,
		int deductedCredits
	) {
	}
}
