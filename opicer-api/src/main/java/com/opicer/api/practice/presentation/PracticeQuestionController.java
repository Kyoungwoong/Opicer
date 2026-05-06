package com.opicer.api.practice.presentation;

import com.opicer.api.question.application.QuestionQueryService;
import com.opicer.api.question.domain.Question;
import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.presentation.ApiResponse;
import com.opicer.api.topic.application.TopicQueryService;
import com.opicer.api.topic.domain.Topic;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/practice/topics")
public class PracticeQuestionController {

	private final TopicQueryService topicQueryService;
	private final QuestionQueryService questionQueryService;

	public PracticeQuestionController(TopicQueryService topicQueryService, QuestionQueryService questionQueryService) {
		this.topicQueryService = topicQueryService;
		this.questionQueryService = questionQueryService;
	}

	@GetMapping("/{topicId}/questions")
	public ResponseEntity<ApiResponse<List<QuestionResponse>>> list(@PathVariable UUID topicId) {
		Topic topic = topicQueryService.getActiveOrThrow(topicId);
		List<QuestionResponse> questions = questionQueryService.findActiveByTopic(topic.getTitle()).stream()
			.map(QuestionResponse::from)
			.toList();
		return ResponseEntity.ok(ApiResponse.ok("PRACTICE_QUESTION_LIST_OK", questions));
	}

	public record QuestionResponse(
		UUID id,
		String topic,
		QuestionType type,
		String promptText,
		String promptAudioUrl,
		String structuralHint,
		List<OpicLevel> targetLevels,
		List<String> keyExpressions,
		Instant createdAt
	) {
		static QuestionResponse from(Question q) {
			return new QuestionResponse(
				q.getId(),
				q.getTopic(),
				q.getType(),
				q.getPromptText(),
				q.getPromptAudioUrl(),
				q.getStructuralHint(),
				q.getTargetLevels(),
				q.getKeyExpressions(),
				q.getCreatedAt()
			);
		}
	}
}
