package com.opicer.api.question.presentation;

import com.opicer.api.question.application.QuestionService;
import com.opicer.api.question.domain.Question;
import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

	private final QuestionService questionService;

	public AdminQuestionController(QuestionService questionService) {
		this.questionService = questionService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<QuestionResponse>>> list() {
		List<QuestionResponse> questions = questionService.findAll().stream()
			.map(QuestionResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok("QUESTION_LIST_OK", questions));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<QuestionResponse>> create(@Valid @RequestBody QuestionRequest request) {
		Question question = questionService.create(
			request.topic(), request.type(), request.promptText(), request.promptAudioUrl(),
			request.structuralHint(), request.targetLevels(), request.keyExpressions());
		return ResponseEntity.status(201)
			.body(ApiResponse.created("QUESTION_CREATED", QuestionResponse.from(question)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Object> update(@PathVariable UUID id, @Valid @RequestBody QuestionRequest request) {
		return questionService.update(id, request.topic(), request.type(), request.promptText(),
				request.promptAudioUrl(), request.structuralHint(), request.targetLevels(),
				request.keyExpressions(), request.active() != null ? request.active() : true)
			.<ResponseEntity<Object>>map(q -> ResponseEntity.ok(
				ApiResponse.ok("QUESTION_UPDATED", QuestionResponse.from(q))))
			.orElseThrow(() -> new ApiException(
				ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Object> delete(@PathVariable UUID id) {
		if (questionService.delete(id)) {
			return ResponseEntity.ok(ApiResponse.ok("QUESTION_DELETED", null));
		}
		throw new ApiException(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + id);
	}

	public record QuestionRequest(
		@NotBlank String topic,
		@NotNull QuestionType type,
		@NotBlank String promptText,
		String promptAudioUrl,
		String structuralHint,
		List<OpicLevel> targetLevels,
		List<String> keyExpressions,
		Boolean active
	) {}

	public record QuestionResponse(
		UUID id,
		String topic,
		QuestionType type,
		String promptText,
		String promptAudioUrl,
		String structuralHint,
		List<OpicLevel> targetLevels,
		List<String> keyExpressions,
		boolean active,
		Instant createdAt,
		Instant updatedAt
	) {
		static QuestionResponse from(Question q) {
			return new QuestionResponse(q.getId(), q.getTopic(), q.getType(), q.getPromptText(),
				q.getPromptAudioUrl(), q.getStructuralHint(), q.getTargetLevels(),
				q.getKeyExpressions(), q.isActive(), q.getCreatedAt(), q.getUpdatedAt());
		}
	}
}
