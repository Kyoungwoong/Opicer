package com.opicer.api.evaluation.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opicer.api.config.AiProperties;
import com.opicer.api.config.EvaluationProperties;
import com.opicer.api.evaluation.application.AnswerEvaluationModel;
import com.opicer.api.evaluation.application.EvaluationFailure;
import com.opicer.api.evaluation.application.EvaluationOutput;
import com.opicer.api.evaluation.application.EvaluationPrompt;
import com.opicer.api.evaluation.domain.CompetencyDimension;
import com.opicer.api.evaluation.domain.CorrectionCategory;
import com.opicer.api.evaluation.domain.EvaluationMode;
import com.opicer.api.shared.domain.OpicLevel;
import java.time.Duration;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Evaluates an answer with the OpenAI Responses API, constrained by a strict JSON schema
 * ({@code text.format = json_schema, strict: true}). The rubric goes in {@code instructions}, which is the same
 * for every call so OpenAI's automatic prompt caching can reuse it. Active with
 * {@code opicer.ai.evaluation.provider=openai} (the default).
 */
@Component
@ConditionalOnProperty(name = EvaluationProperties.PROVIDER_KEY, havingValue = "openai", matchIfMissing = true)
public class OpenAiAnswerEvaluationModel implements AnswerEvaluationModel {

	private static final Logger log = LoggerFactory.getLogger(OpenAiAnswerEvaluationModel.class);
	static final String SCHEMA_NAME = "answer_evaluation";
	static final String COACH_SCHEMA_NAME = "answer_coaching";
	/** JSON schema of {@link EvaluationOutput}; enum values come from the domain enums so they never drift. */
	static final ObjectNode SCHEMA = schema(EvaluationMode.FULL);
	/** The coaching subset: no competency ratings and no strengths. */
	static final ObjectNode COACH_SCHEMA = schema(EvaluationMode.COACH);

	private final AiProperties aiProperties;
	private final EvaluationProperties properties;
	private final RestClient restClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	public OpenAiAnswerEvaluationModel(AiProperties aiProperties, EvaluationProperties properties,
		RestClient.Builder builder) {
		this(aiProperties, properties, builder.requestFactory(timeouts(properties)).build());
	}

	/** For tests: a client already bound to a fake server. */
	OpenAiAnswerEvaluationModel(AiProperties aiProperties, EvaluationProperties properties, RestClient restClient) {
		this.aiProperties = aiProperties;
		this.properties = properties;
		this.restClient = restClient;
	}

	@Override
	public EvaluationOutput evaluate(EvaluationPrompt prompt) {
		String apiKey = aiProperties.getOpenaiApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw EvaluationFailure.permanent("NOT_CONFIGURED", "OPENAI_API_KEY is not set");
		}
		JsonNode response = call(apiKey, requestBody(prompt));
		return parse(response);
	}

	@Override
	public String provider() {
		return "openai";
	}

	@Override
	public String model() {
		return properties.getOpenaiModel();
	}

	ObjectNode requestBody(EvaluationPrompt prompt) {
		ObjectNode body = JsonNodeFactory.instance.objectNode();
		body.put("model", properties.getOpenaiModel());
		body.put("instructions", prompt.instructions());
		body.put("input", prompt.input());
		body.put("max_output_tokens", properties.getMaxOutputTokens());
		body.putObject("reasoning").put("effort", properties.getReasoningEffort());
		ObjectNode format = body.putObject("text").putObject("format");
		format.put("type", "json_schema");
		boolean coach = prompt.mode() == EvaluationMode.COACH;
		format.put("name", coach ? COACH_SCHEMA_NAME : SCHEMA_NAME);
		format.put("strict", true);
		format.set("schema", coach ? COACH_SCHEMA : SCHEMA);
		// Stored responses would keep learner transcripts on the provider side; we do not need them.
		body.put("store", false);
		return body;
	}

	private JsonNode call(String apiKey, ObjectNode body) {
		try {
			return restClient.post()
				.uri(properties.getOpenaiUrl())
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(JsonNode.class);
		} catch (HttpClientErrorException e) {
			int status = e.getStatusCode().value();
			if (status == 429) {
				throw EvaluationFailure.retryable("RATE_LIMITED", null);
			}
			if (status == 401 || status == 403) {
				throw EvaluationFailure.permanent("NOT_AUTHORIZED", "HTTP " + status);
			}
			// 400/404 (e.g. unknown model, invalid schema): retrying the same request cannot succeed.
			log.error("[Eval:openai] request rejected: HTTP {} {}", status, e.getResponseBodyAsString());
			throw EvaluationFailure.permanent("BAD_REQUEST", "HTTP " + status);
		} catch (HttpServerErrorException e) {
			throw EvaluationFailure.retryable("PROVIDER_ERROR", "HTTP " + e.getStatusCode().value());
		} catch (ResourceAccessException e) {
			throw EvaluationFailure.retryable("TIMEOUT", e.getMessage());
		} catch (RestClientException e) {
			throw EvaluationFailure.retryable("PROVIDER_ERROR", e.getMessage());
		}
	}

	/** Refusals are permanent; truncation and unparseable output are worth another attempt. */
	EvaluationOutput parse(JsonNode response) {
		if (response == null) {
			throw EvaluationFailure.retryable("INVALID_OUTPUT", "empty response");
		}
		logUsage(response.path("usage"));
		if ("incomplete".equals(response.path("status").asText())) {
			throw EvaluationFailure.retryable("TRUNCATED", response.path("incomplete_details").path("reason").asText());
		}
		String text = null;
		for (JsonNode item : response.path("output")) {
			if (!"message".equals(item.path("type").asText())) {
				continue;
			}
			for (JsonNode content : item.path("content")) {
				String type = content.path("type").asText();
				if ("refusal".equals(type)) {
					throw EvaluationFailure.permanent("REFUSED", content.path("refusal").asText());
				}
				if ("output_text".equals(type)) {
					text = content.path("text").asText();
				}
			}
		}
		if (text == null || text.isBlank()) {
			throw EvaluationFailure.retryable("INVALID_OUTPUT", "no output text");
		}
		try {
			return objectMapper.readValue(text, EvaluationOutput.class);
		} catch (JsonProcessingException e) {
			throw EvaluationFailure.retryable("INVALID_OUTPUT", e.getOriginalMessage());
		}
	}

	/** Token usage per call, to calibrate {@code max-output-tokens} and watch prompt-cache hits. */
	private void logUsage(JsonNode usage) {
		if (usage.isMissingNode()) {
			return;
		}
		log.info("[Eval:openai] model={} input={} cached={} output={} reasoning={}", properties.getOpenaiModel(),
			usage.path("input_tokens").asInt(), usage.path("input_tokens_details").path("cached_tokens").asInt(),
			usage.path("output_tokens").asInt(), usage.path("output_tokens_details").path("reasoning_tokens").asInt());
	}

	private static SimpleClientHttpRequestFactory timeouts(EvaluationProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(10));
		factory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
		return factory;
	}

	/** Strict-mode schema: every object lists all its properties as required and forbids extra ones. */
	private static ObjectNode schema(EvaluationMode mode) {
		ObjectNode dimension = object(
			"code", enumOf(names(CompetencyDimension.values())),
			"level", enumOf(names(OpicLevel.values())),
			"evidence", string(),
			"commentKo", string());
		ObjectNode correction = object(
			"original", string(),
			"corrected", string(),
			"reasonKo", string(),
			"category", enumOf(names(CorrectionCategory.values())));
		ObjectNode expression = object(
			"expression", string(),
			"meaningKo", string());
		if (mode == EvaluationMode.COACH) {
			return object(
				"corrections", array(correction),
				"targetScript", string(),
				"keyExpressions", array(expression));
		}
		return object(
			"dimensions", array(dimension),
			"strengths", array(string()),
			"corrections", array(correction),
			"targetScript", string(),
			"keyExpressions", array(expression));
	}

	private static ObjectNode object(Object... namesAndSchemas) {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("type", "object");
		ObjectNode properties = node.putObject("properties");
		ArrayNode required = node.putArray("required");
		for (int i = 0; i < namesAndSchemas.length; i += 2) {
			String name = (String) namesAndSchemas[i];
			properties.set(name, (JsonNode) namesAndSchemas[i + 1]);
			required.add(name);
		}
		node.put("additionalProperties", false);
		return node;
	}

	private static ObjectNode array(JsonNode items) {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("type", "array");
		node.set("items", items);
		return node;
	}

	private static ObjectNode string() {
		return JsonNodeFactory.instance.objectNode().put("type", "string");
	}

	private static ObjectNode enumOf(String... values) {
		ObjectNode node = string();
		ArrayNode list = node.putArray("enum");
		Arrays.stream(values).forEach(list::add);
		return node;
	}

	private static String[] names(Enum<?>[] values) {
		return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
	}
}
