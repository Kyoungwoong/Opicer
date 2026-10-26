package com.opicer.api.practice.presentation;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.practice.application.PracticeHistoryCommandService;
import com.opicer.api.practice.application.PracticeHistoryQueryService;
import com.opicer.api.practice.application.PracticeSessionCommandService;
import com.opicer.api.practice.domain.AnswerEvaluationStatus;
import com.opicer.api.practice.domain.PracticeAnswer;
import com.opicer.api.practice.domain.PracticeSession;
import com.opicer.api.practice.domain.SessionEvaluationStatus;
import com.opicer.api.shared.domain.OpicLevel;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/practice/sessions")
public class PracticeSessionController {

	private final PracticeSessionCommandService practiceSessionCommandService;
	private final PracticeHistoryQueryService practiceHistoryQueryService;
	private final PracticeHistoryCommandService practiceHistoryCommandService;

	public PracticeSessionController(
		PracticeSessionCommandService practiceSessionCommandService,
		PracticeHistoryQueryService practiceHistoryQueryService,
		PracticeHistoryCommandService practiceHistoryCommandService
	) {
		this.practiceSessionCommandService = practiceSessionCommandService;
		this.practiceHistoryQueryService = practiceHistoryQueryService;
		this.practiceHistoryCommandService = practiceHistoryCommandService;
	}

	@PostMapping("/submit")
	public ResponseEntity<ApiResponse<SubmitResponse>> submit(
		Authentication authentication,
		@Valid @RequestBody SubmitRequest request
	) {
		AuthUserPrincipal principal = principalOf(authentication);
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}
		List<AnswerRequest> answers = request.answers() == null ? List.of() : request.answers();
		Set<UUID> questionIds = new HashSet<>();
		for (AnswerRequest answer : answers) {
			if (!questionIds.add(answer.questionId())) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "Duplicate questionId in answers");
			}
		}

		PracticeSessionCommandService.SubmitResult result = practiceSessionCommandService.submit(
			principal.id(),
			request.topicSelectionId(),
			request.targetLevel(),
			answers.stream()
				.map(a -> new PracticeSessionCommandService.AnswerInput(
					a.questionId(), a.questionText(), a.transcript(), a.durationSeconds()))
				.toList()
		);
		return ResponseEntity.ok(ApiResponse.ok("PRACTICE_SUBMITTED", new SubmitResponse(
			result.chargeId(),
			result.alreadyCharged(),
			result.deductedCredits(),
			result.sessionId(),
			result.evaluationStatus()
		)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<SessionSummaryResponse>>> list(
		Authentication authentication,
		@RequestParam(defaultValue = "20") int limit
	) {
		AuthUserPrincipal principal = principalOf(authentication);
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}
		List<SessionSummaryResponse> body = practiceHistoryQueryService.list(principal.id(), limit).stream()
			.map(SessionSummaryResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok("PRACTICE_SESSIONS", body));
	}

	@GetMapping("/{sessionId}")
	public ResponseEntity<ApiResponse<SessionDetailResponse>> get(
		Authentication authentication,
		@PathVariable UUID sessionId
	) {
		AuthUserPrincipal principal = principalOf(authentication);
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}
		return ResponseEntity.ok(ApiResponse.ok("PRACTICE_SESSION",
			SessionDetailResponse.from(practiceHistoryQueryService.get(principal.id(), sessionId))));
	}

	@PutMapping("/{sessionId}/answers/{questionId}")
	public ResponseEntity<ApiResponse<AnswerResponse>> recordAiResult(
		Authentication authentication,
		@PathVariable UUID sessionId,
		@PathVariable UUID questionId,
		@Valid @RequestBody AiResultRequest request
	) {
		AuthUserPrincipal principal = principalOf(authentication);
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}
		if (request.analysis() == null && request.improvedScript() == null) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "analysis or improvedScript is required");
		}
		PracticeAnswer answer = practiceHistoryCommandService.recordAiResult(
			principal.id(), sessionId, questionId, request.analysis(), request.improvedScript()
		);
		return ResponseEntity.ok(ApiResponse.ok("PRACTICE_ANSWER_UPDATED", AnswerResponse.from(answer)));
	}

	@DeleteMapping("/{sessionId}")
	public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID sessionId) {
		AuthUserPrincipal principal = principalOf(authentication);
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}
		practiceHistoryCommandService.delete(principal.id(), sessionId);
		return ResponseEntity.noContent().build();
	}

	private static AuthUserPrincipal principalOf(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof AuthUserPrincipal principal) {
			return principal;
		}
		return null;
	}

	/** @param targetLevel IM, IH or AL; null means IM */
	public record SubmitRequest(
		@NotNull UUID topicSelectionId,
		OpicLevel targetLevel,
		@Size(max = 20) List<@Valid AnswerRequest> answers
	) {
	}

	public record AnswerRequest(
		@NotNull UUID questionId,
		@NotBlank @Size(max = 2000) String questionText,
		@Size(max = 10000) String transcript,
		@Min(1) @Max(600) Integer durationSeconds
	) {
	}

	public record AiResultRequest(
		@Size(max = 20000) String analysis,
		@Size(max = 10000) String improvedScript
	) {
	}

	public record SubmitResponse(
		UUID chargeId,
		boolean alreadyCharged,
		int deductedCredits,
		UUID sessionId,
		SessionEvaluationStatus evaluationStatus
	) {
	}

	public record SessionSummaryResponse(
		UUID id,
		UUID topicId,
		String topicTitle,
		Instant startedAt,
		Instant completedAt,
		int answerCount,
		int analyzedCount,
		SessionEvaluationStatus evaluationStatus,
		OpicLevel estimatedLevel
	) {
		static SessionSummaryResponse from(PracticeHistoryQueryService.SessionSummary summary) {
			PracticeSession s = summary.session();
			return new SessionSummaryResponse(
				s.getId(), s.getTopicId(), s.getTopicTitle(), s.getStartedAt(), s.getCompletedAt(),
				summary.answerCount(), summary.analyzedCount(), s.getEvaluationStatus(), s.getEstimatedLevel()
			);
		}
	}

	public record SessionDetailResponse(
		UUID id,
		UUID topicId,
		String topicTitle,
		Instant startedAt,
		Instant completedAt,
		OpicLevel targetLevel,
		SessionEvaluationStatus evaluationStatus,
		OpicLevel estimatedLevel,
		@JsonRawValue String report,
		Instant evaluatedAt,
		List<AnswerResponse> answers
	) {
		static SessionDetailResponse from(PracticeHistoryQueryService.SessionDetail detail) {
			PracticeSession s = detail.session();
			return new SessionDetailResponse(
				s.getId(), s.getTopicId(), s.getTopicTitle(), s.getStartedAt(), s.getCompletedAt(),
				s.getTargetLevel(), s.getEvaluationStatus(), s.getEstimatedLevel(), s.getReport(), s.getEvaluatedAt(),
				detail.answers().stream().map(AnswerResponse::from).toList()
			);
		}
	}

	public record AnswerResponse(
		UUID questionId,
		int questionOrder,
		String questionText,
		String transcript,
		String analysis,
		String improvedScript,
		Integer durationSeconds,
		AnswerEvaluationStatus evaluationStatus,
		OpicLevel overallLevel,
		@JsonRawValue String evaluation
	) {
		static AnswerResponse from(PracticeAnswer answer) {
			return new AnswerResponse(
				answer.getQuestionId(), answer.getQuestionOrder(), answer.getQuestionText(),
				answer.getTranscript(), answer.getAnalysis(), answer.getImprovedScript(),
				answer.getDurationSeconds(), answer.getEvaluationStatus(), answer.getOverallLevel(), answer.getEvaluation()
			);
		}
	}
}
