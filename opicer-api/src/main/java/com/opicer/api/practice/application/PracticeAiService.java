package com.opicer.api.practice.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opicer.api.config.AiProperties;
import com.opicer.api.prompt.application.PromptVersionService;
import com.opicer.api.prompt.domain.PromptUseCase;
import com.opicer.api.prompt.domain.PromptVersion;
import com.opicer.api.goodanswer.application.GoodAnswerSampleService;
import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.io.InputStream;
import java.util.StringJoiner;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
	private final PromptVersionService promptVersionService;
	private final GoodAnswerSampleService goodAnswerSampleService;
	private final RestClient restClient;

	public PracticeAiService(
		AiProperties aiProperties,
		PromptVersionService promptVersionService,
		GoodAnswerSampleService goodAnswerSampleService
	) {
		this.aiProperties = aiProperties;
		this.promptVersionService = promptVersionService;
		this.goodAnswerSampleService = goodAnswerSampleService;
		this.restClient = RestClient.create();
	}

	public String transcribe(MultipartFile audio, String questionText) {
		String apiKey = aiProperties.getOpenaiApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw new ApiException(ErrorCode.AI_NOT_CONFIGURED);
		}
		try {
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", new MultipartInputStreamFileResource(audio.getInputStream(), "audio.webm"));
			body.add("model", "whisper-1");
			body.add("response_format", "text");

			return restClient.post()
				.uri("https://api.openai.com/v1/audio/transcriptions")
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(body)
				.retrieve()
				.body(String.class);
		} catch (RestClientException e) {
			log.error("[Whisper] API call failed: {}", e.getMessage(), e);
			throw new ApiException(ErrorCode.AI_TRANSCRIPTION_FAILED, e.getMessage());
		} catch (Exception e) {
			log.error("[Whisper] Unexpected error: {}", e.getMessage(), e);
			throw new ApiException(ErrorCode.AI_TRANSCRIPTION_FAILED, e.getMessage());
		}
	}

	public String analyze(UUID topicId, String questionText, String transcript) {
		String apiKey = aiProperties.getAnthropicApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw new ApiException(ErrorCode.AI_NOT_CONFIGURED);
		}
		String template = promptVersionService.findActiveByUseCase(PromptUseCase.FEEDBACK)
			.map(PromptVersion::getTemplate)
			.orElse(DEFAULT_FEEDBACK_TEMPLATE);
		String prompt = template
			.replace("{questionText}", questionText)
			.replace("{transcript}", transcript);
		prompt = attachRagContext(prompt, topicId, transcript);
		return callClaude(apiKey, prompt);
	}

	public String improve(UUID topicId, String questionText, String transcript) {
		String apiKey = aiProperties.getAnthropicApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw new ApiException(ErrorCode.AI_NOT_CONFIGURED);
		}
		String template = promptVersionService.findActiveByUseCase(PromptUseCase.SCRIPT_IMPROVEMENT)
			.map(PromptVersion::getTemplate)
			.orElse(DEFAULT_SCRIPT_IMPROVEMENT_TEMPLATE);
		String prompt = template
			.replace("{questionText}", questionText)
			.replace("{transcript}", transcript);
		prompt = attachRagContext(prompt, topicId, transcript);
		return callClaude(apiKey, prompt);
	}

	private String attachRagContext(String prompt, UUID topicId, String transcript) {
		List<GoodAnswerSample> samples = goodAnswerSampleService.findSimilar(topicId, transcript, 3);
		if (samples.isEmpty()) return prompt;
		StringJoiner joiner = new StringJoiner("\n\n");
		joiner.add("### Similar Good Answer Samples");
		for (int i = 0; i < samples.size(); i++) {
			GoodAnswerSample sample = samples.get(i);
			joiner.add(String.format(
				"%d) Level: %s\nSummary: %s\nKey Expressions: %s\nSample: %s",
				i + 1,
				sample.getLevel(),
				sample.getSummary() == null ? "-" : sample.getSummary(),
				sample.getKeyExpressions() == null ? "-" : String.join(", ", sample.getKeyExpressions()),
				sample.getSampleText()
			));
		}
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

	private static class MultipartInputStreamFileResource extends InputStreamResource {

		private final String filename;

		MultipartInputStreamFileResource(InputStream in, String filename) {
			super(in);
			this.filename = filename;
		}

		@Override
		public String getFilename() {
			return filename;
		}

		@Override
		public long contentLength() {
			return -1;
		}
	}
}
