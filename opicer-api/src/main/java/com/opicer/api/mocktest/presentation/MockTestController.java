package com.opicer.api.mocktest.presentation;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.mocktest.application.MockTestCommandService;
import com.opicer.api.mocktest.application.MockTestCommandService.AnswerInput;
import com.opicer.api.mocktest.application.MockTestCommandService.Composed;
import com.opicer.api.mocktest.application.MockTestCommandService.Submitted;
import com.opicer.api.mocktest.application.MockTestQueryService;
import com.opicer.api.mocktest.domain.DifficultyAdjustment;
import com.opicer.api.mocktest.domain.MockTest;
import com.opicer.api.mocktest.domain.MockTestItem;
import com.opicer.api.mocktest.domain.MockTestStatus;
import com.opicer.api.mocktest.domain.SurveyCatalog;
import com.opicer.api.practice.domain.AnswerEvaluationStatus;
import com.opicer.api.practice.domain.SessionEvaluationStatus;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The learner's mock tests (실전연습). Every endpoint works on the signed-in user's own tests only. */
@RestController
@RequestMapping("/api/mock-tests")
public class MockTestController {

	private final MockTestCommandService commandService;
	private final MockTestQueryService queryService;

	public MockTestController(MockTestCommandService commandService, MockTestQueryService queryService) {
		this.commandService = commandService;
		this.queryService = queryService;
	}

	@GetMapping("/survey")
	public ResponseEntity<ApiResponse<SurveyResponse>> survey() {
		return ResponseEntity.ok(ApiResponse.ok("MOCK_TEST_SURVEY",
			new SurveyResponse(SurveyCatalog.PARTS, SurveyCatalog.PART4_MIN_TOTAL, MockTestCommandService.MOCK_TEST_COST)));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<StartResponse>> start(Authentication authentication,
		@Valid @RequestBody StartRequest request) {
		UUID userId = userId(authentication);
		Composed composed = commandService.start(userId, request.survey(), request.selfAssessment(), request.targetLevel());
		return ResponseEntity.status(201).body(ApiResponse.created("MOCK_TEST_STARTED",
			new StartResponse(composed.test().getId(), items(composed.items()))));
	}

	@PostMapping("/{id}/adjust")
	public ResponseEntity<ApiResponse<List<ItemResponse>>> adjust(Authentication authentication, @PathVariable UUID id,
		@Valid @RequestBody AdjustRequest request) {
		Composed composed = commandService.adjust(userId(authentication), id, request.difficulty());
		return ResponseEntity.ok(ApiResponse.ok("MOCK_TEST_ADJUSTED", items(composed.items())));
	}

	@PostMapping("/{id}/submit")
	public ResponseEntity<ApiResponse<Submitted>> submit(Authentication authentication, @PathVariable UUID id,
		@Valid @RequestBody SubmitRequest request) {
		List<AnswerInput> answers = request.answers() == null ? List.of() : request.answers().stream()
			.map(a -> new AnswerInput(a.itemId(), a.transcript(), a.durationSeconds()))
			.toList();
		return ResponseEntity.ok(ApiResponse.ok("MOCK_TEST_SUBMITTED",
			commandService.submit(userId(authentication), id, answers, request.examSeconds())));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<SummaryResponse>>> list(Authentication authentication,
		@RequestParam(defaultValue = "20") int limit) {
		return ResponseEntity.ok(ApiResponse.ok("MOCK_TESTS", queryService.list(userId(authentication), limit).stream()
			.map(s -> SummaryResponse.from(s.test(), s.itemCount()))
			.toList()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<DetailResponse>> get(Authentication authentication, @PathVariable UUID id) {
		MockTestQueryService.Detail detail = queryService.get(userId(authentication), id);
		return ResponseEntity.ok(ApiResponse.ok("MOCK_TEST", DetailResponse.from(detail.test(), detail.items())));
	}

	@PostMapping("/{id}/evaluation/retry")
	public ResponseEntity<ApiResponse<RetryResponse>> retry(Authentication authentication, @PathVariable UUID id) {
		SessionEvaluationStatus status = commandService.retry(userId(authentication), id);
		return ResponseEntity.accepted().body(ApiResponse.of(HttpStatus.ACCEPTED, "EVALUATION_RETRY_QUEUED",
			new RetryResponse(id, status)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
		commandService.delete(userId(authentication), id);
		return ResponseEntity.noContent().build();
	}

	/** 401 for any authentication that is not an Opicer sign-in, like the practice endpoints. */
	private static UUID userId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof AuthUserPrincipal principal) {
			return principal.id();
		}
		throw new ApiException(ErrorCode.UNAUTHORIZED);
	}

	private static List<ItemResponse> items(List<MockTestItem> items) {
		return items.stream().map(ItemResponse::from).toList();
	}

	public record SurveyResponse(List<SurveyCatalog.SurveyPart> parts, int part4MinTotal, int cost) {}

	/** @param targetLevel IM, IH or AL for the report's gap; null means IM */
	public record StartRequest(
		@NotNull Map<String, List<String>> survey,
		@Min(1) @Max(6) int selfAssessment,
		OpicLevel targetLevel
	) {}

	public record StartResponse(UUID id, List<ItemResponse> items) {}

	public record AdjustRequest(@NotNull DifficultyAdjustment difficulty) {}

	public record SubmitRequest(
		@Size(max = 20) List<@Valid AnswerRequest> answers,
		@Min(0) @Max(7200) Integer examSeconds
	) {}

	public record AnswerRequest(
		@NotNull UUID itemId,
		@Size(max = 10000) String transcript,
		@Min(1) @Max(600) Integer durationSeconds
	) {}

	/** What the exam screen needs to ask one question. */
	public record ItemResponse(UUID id, int order, int part, String questionText, String questionType,
		String promptAudioUrl) {
		static ItemResponse from(MockTestItem item) {
			return new ItemResponse(item.getId(), item.getItemOrder(), item.getPart(), item.getQuestionText(),
				item.getQuestionType(), item.getPromptAudioUrl());
		}
	}

	public record SummaryResponse(UUID id, Instant startedAt, Instant submittedAt, int selfAssessment,
		DifficultyAdjustment adjustment, OpicLevel targetLevel, int itemCount, SessionEvaluationStatus evaluationStatus,
		OpicLevel estimatedLevel) {
		static SummaryResponse from(MockTest t, int itemCount) {
			return new SummaryResponse(t.getId(), t.getStartedAt(), t.getSubmittedAt(), t.getSelfAssessment(),
				t.getAdjustment(), t.getTargetLevel(), itemCount, t.getEvaluationStatus(), t.getEstimatedLevel());
		}
	}

	public record DetailResponse(UUID id, MockTestStatus status, @JsonRawValue String survey, int selfAssessment,
		DifficultyAdjustment adjustment, OpicLevel targetLevel, Instant startedAt, Instant submittedAt, Integer examSeconds,
		SessionEvaluationStatus evaluationStatus, OpicLevel estimatedLevel, @JsonRawValue String report,
		@JsonRawValue String insights, Instant evaluatedAt, List<ItemDetailResponse> items) {
		static DetailResponse from(MockTest t, List<MockTestItem> items) {
			return new DetailResponse(t.getId(), t.getStatus(), t.getSurvey(), t.getSelfAssessment(), t.getAdjustment(),
				t.getTargetLevel(), t.getStartedAt(), t.getSubmittedAt(), t.getExamSeconds(), t.getEvaluationStatus(),
				t.getEstimatedLevel(), t.getReport(), t.getInsights(), t.getEvaluatedAt(),
				items.stream().map(ItemDetailResponse::from).toList());
		}
	}

	public record ItemDetailResponse(UUID id, int order, int part, String questionText, String questionType,
		String topicTitle, String transcript, Integer durationSeconds, AnswerEvaluationStatus evaluationStatus,
		OpicLevel overallLevel, @JsonRawValue String evaluation) {
		static ItemDetailResponse from(MockTestItem i) {
			return new ItemDetailResponse(i.getId(), i.getItemOrder(), i.getPart(), i.getQuestionText(), i.getQuestionType(),
				i.getTopicTitle(), i.getTranscript(), i.getDurationSeconds(), i.getEvaluationStatus(), i.getOverallLevel(),
				i.getEvaluation());
		}
	}

	public record RetryResponse(UUID mockTestId, SessionEvaluationStatus evaluationStatus) {}
}
