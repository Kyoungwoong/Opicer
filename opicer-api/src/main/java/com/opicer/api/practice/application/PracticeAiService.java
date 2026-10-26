package com.opicer.api.practice.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opicer.api.ai.application.AiTranscriptionService;
import com.opicer.api.config.AiProperties;
import com.opicer.api.prompt.application.PromptVersionQueryService;
import com.opicer.api.prompt.domain.PromptUseCase;
import com.opicer.api.prompt.domain.PromptVersion;
import com.opicer.api.goodanswer.application.GoodAnswerSampleQueryService;
import com.opicer.api.goodanswer.application.ReferenceSample;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.StringJoiner;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PracticeAiService {

	private static final Logger log = LoggerFactory.getLogger(PracticeAiService.class);

	private static final String DEFAULT_FEEDBACK_TEMPLATE =
		"You are an OPIC speaking coach. Analyze this student's response in Korean.\n" +
		"Question: {questionText}\n" +
		"Student's answer: {transcript}\n" +
		"Provide analysis: 1.내용구성 2.표현력 3.레벨평가(NL/IL/IM/IH/AL) 4.개선포인트 top3";

	private static final String DEFAULT_SCRIPT_IMPROVEMENT_TEMPLATE =
		"You are an OPIC speaking coach. Improve this answer, preserving personal content.\n" +
		"Question: {questionText}\n" +
		"Original: {transcript}\n" +
		"Return ONLY the improved script. Target IL~IM level.";

	private final AiProperties aiProperties;
	private final PromptVersionQueryService promptVersionQueryService;
	private final GoodAnswerSampleQueryService goodAnswerSampleQueryService;
	private final AiTranscriptionService transcriptionService;
	private final RestClient restClient;

	public PracticeAiService(
		AiProperties aiProperties,
		PromptVersionQueryService promptVersionQueryService,
		GoodAnswerSampleQueryService goodAnswerSampleQueryService,
		AiTranscriptionService transcriptionService
	) {
		this.aiProperties = aiProperties;
		this.promptVersionQueryService = promptVersionQueryService;
		this.goodAnswerSampleQueryService = goodAnswerSampleQueryService;
		this.transcriptionService = transcriptionService;
		this.restClient = RestClient.create();
	}

	/** Delegates to the configured speech-to-text provider; the question text is passed as a hint. */
	public String transcribe(MultipartFile audio, String questionText) {
		return transcriptionService.transcribe(audio, questionText);
	}

	public String analyze(
		UUID topicId,
		String questionText,
		String transcript,
		String questionType,
		OpicLevel targetLevel
	) {
		String apiKey = aiProperties.getAnthropicApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw new ApiException(ErrorCode.AI_NOT_CONFIGURED);
		}
		String template = promptVersionQueryService.findActiveByUseCase(PromptUseCase.FEEDBACK)
			.map(PromptVersion::getTemplate)
			.orElse(DEFAULT_FEEDBACK_TEMPLATE);
		String prompt = template
			.replace("{questionText}", questionText)
			.replace("{transcript}", transcript);
		prompt = attachRagContext(prompt, topicId, transcript);
		return callClaude(apiKey, prompt);
	}

	public String improve(
		UUID topicId,
		String questionText,
		String transcript,
		String questionType,
		OpicLevel targetLevel
	) {
		String apiKey = aiProperties.getAnthropicApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw new ApiException(ErrorCode.AI_NOT_CONFIGURED);
		}
		String template = promptVersionQueryService.findActiveByUseCase(PromptUseCase.SCRIPT_IMPROVEMENT)
			.map(PromptVersion::getTemplate)
			.orElse(DEFAULT_SCRIPT_IMPROVEMENT_TEMPLATE);
		String prompt = template
			.replace("{questionText}", questionText)
			.replace("{transcript}", transcript);
		prompt = attachRagContext(prompt, topicId, transcript);
		return callClaude(apiKey, prompt);
	}

	/** Reference samples only enrich the prompt, so a retrieval failure degrades to "no samples". */
	List<ReferenceSample> referenceSamplesOrEmpty(UUID topicId, String transcript) {
		try {
			return goodAnswerSampleQueryService.findReferenceSamples(topicId, transcript);
		} catch (ApiException e) {
			log.warn("[RAG] reference retrieval skipped: {}", e.getErrorCode());
			return List.of();
		}
	}

	private String attachRagContext(String prompt, UUID topicId, String transcript) {
		List<ReferenceSample> samples = referenceSamplesOrEmpty(topicId, transcript);
		if (samples.isEmpty()) return prompt;
		StringJoiner joiner = new StringJoiner("\n\n");
		joiner.add("### RAG Reference Samples");
		joiner.add("""
			Use these references only as guidance.
			- Do not copy sentences verbatim.
			- Rebuild the answer in the learner's own words and context.
			- Prioritize structure, flow, and expressions over memorization.
			- If references conflict with learner context, prefer learner context.
			""");
		for (int i = 0; i < samples.size(); i++) {
			ReferenceSample sample = samples.get(i);
			joiner.add(String.format(
				"%d) Level: %s\nSummary: %s\nKey Expressions: %s\nSample: %s",
				i + 1,
				sample.level(),
				sample.summary() == null ? "-" : sample.summary(),
				sample.keyExpressions().isEmpty() ? "-" : String.join(", ", sample.keyExpressions()),
				sample.sampleText()
			));
		}
		joiner.add("""
			### Output Guardrails
			- Never output exact copied chunks from references.
			- Keep response grounded to the given question.
			- Keep explanation in Korean.
			""");
		return prompt + "\n\n" + joiner;
	}

	private String callClaude(String apiKey, String prompt) {
		try {
			Map<String, Object> req = Map.of(
				"model", "claude-opus-4-6",
				"max_tokens", 1024,
				"messages", List.of(Map.of("role", "user", "content", prompt))
			);
			ClaudeResponse response = restClient.post()
				.uri("https://api.anthropic.com/v1/messages")
				.header("x-api-key", apiKey)
				.header("anthropic-version", "2023-06-01")
				.contentType(MediaType.APPLICATION_JSON)
				.body(req)
				.retrieve()
				.body(ClaudeResponse.class);
			if (response == null || response.content() == null || response.content().isEmpty()) {
				throw new ApiException(ErrorCode.AI_ANALYSIS_FAILED, "Empty response from AI");
			}
			return response.content().get(0).text();
		} catch (ApiException e) {
			throw e;
		} catch (RestClientException e) {
			log.error("[Claude] API call failed: {}", e.getMessage(), e);
			throw new ApiException(ErrorCode.AI_ANALYSIS_FAILED, e.getMessage());
		} catch (Exception e) {
			log.error("[Claude] Unexpected error: {}", e.getMessage(), e);
			throw new ApiException(ErrorCode.AI_ANALYSIS_FAILED, e.getMessage());
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ClaudeResponse(List<ContentBlock> content) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ContentBlock(String type, String text) {}
}
