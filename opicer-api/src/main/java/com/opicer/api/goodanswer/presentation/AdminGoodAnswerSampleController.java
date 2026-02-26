package com.opicer.api.goodanswer.presentation;

import com.opicer.api.goodanswer.application.GoodAnswerSampleService;
import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/good-answers")
public class AdminGoodAnswerSampleController {

	private final GoodAnswerSampleService service;

	public AdminGoodAnswerSampleController(GoodAnswerSampleService service) {
		this.service = service;
	}

	@PostMapping
	public ApiResponse<Map<String, Object>> create(@Valid @RequestBody CreateRequest request) {
		GoodAnswerSample sample = service.create(
			request.topicId(),
			request.level(),
			request.sampleText(),
			request.sampleAudioUrl(),
			request.summary(),
			request.tags(),
			request.keyExpressions()
		);
		return ApiResponse.ok("Good answer sample created", Map.of(
			"id", sample.getId(),
			"createdAt", sample.getCreatedAt(),
			"updatedAt", sample.getUpdatedAt()
		));
	}

	@GetMapping
	public ApiResponse<List<GoodAnswerSampleResponse>> list(@RequestParam UUID topicId) {
		List<GoodAnswerSampleResponse> result = service.listByTopic(topicId).stream()
			.map(GoodAnswerSampleResponse::from)
			.toList();
		return ApiResponse.ok("Good answer samples", result);
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ApiResponse.ok("Good answer sample deleted", null);
	}

	record CreateRequest(
		@NotNull UUID topicId,
		@NotNull OpicLevel level,
		@NotBlank String sampleText,
		String sampleAudioUrl,
		String summary,
		List<String> tags,
		List<String> keyExpressions
	) {}

	record GoodAnswerSampleResponse(
		UUID id,
		UUID topicId,
		OpicLevel level,
		String sampleText,
		String sampleAudioUrl,
		String summary,
		List<String> tags,
		List<String> keyExpressions,
		String createdAt
	) {
		static GoodAnswerSampleResponse from(GoodAnswerSample sample) {
			return new GoodAnswerSampleResponse(
				sample.getId(),
				sample.getTopic().getId(),
				sample.getLevel(),
				sample.getSampleText(),
				sample.getSampleAudioUrl(),
				sample.getSummary(),
				sample.getTags(),
				sample.getKeyExpressions(),
				sample.getCreatedAt().toString()
			);
		}
	}
}
