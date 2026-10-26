package com.opicer.api.evaluation.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opicer.api.config.AiProperties;
import com.opicer.api.config.EvaluationProperties;
import com.opicer.api.evaluation.application.EvaluationFailure;
import com.opicer.api.evaluation.application.EvaluationOutput;
import com.opicer.api.evaluation.application.EvaluationPrompt;
import com.opicer.api.evaluation.domain.CompetencyDimension;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Contract test against a fake HTTP server; no real OpenAI call. */
class OpenAiAnswerEvaluationModelTest {

	private static final EvaluationPrompt PROMPT = new EvaluationPrompt("rubric instructions", "answer input");
	private static final String OUTPUT_JSON = """
		{"dimensions":[{"code":"TASK","level":"IM","evidence":"I go","commentKo":"좋아요"},
		 {"code":"DISCOURSE","level":"IM","evidence":"","commentKo":""},
		 {"code":"ACCURACY","level":"IL","evidence":"","commentKo":""},
		 {"code":"VOCABULARY","level":"IM","evidence":"","commentKo":""}],
		 "strengths":["좋아요"],
		 "corrections":[{"original":"I go","corrected":"I went","reasonKo":"과거","category":"TENSE"}],
		 "targetScript":"I went there.",
		 "keyExpressions":[{"expression":"Looking back","meaningKo":"돌이켜 보면"}]}
		""";

	private final ObjectMapper json = new ObjectMapper();
	private final AiProperties aiProperties = new AiProperties();
	private final EvaluationProperties properties = new EvaluationProperties();
	private MockRestServiceServer server;
	private OpenAiAnswerEvaluationModel model;

	@BeforeEach
	void setUp() {
		aiProperties.setOpenaiApiKey("test-key");
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		model = new OpenAiAnswerEvaluationModel(aiProperties, properties, builder.build());
	}

	@Test
	void sendsAStrictJsonSchemaRequestAndParsesTheOutput() throws Exception {
		server.expect(requestTo(properties.getOpenaiUrl()))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("Authorization", "Bearer test-key"))
			.andExpect(jsonPath("$.model").value("gpt-5.6-sol"))
			.andExpect(jsonPath("$.instructions").value("rubric instructions"))
			.andExpect(jsonPath("$.input").value("answer input"))
			.andExpect(jsonPath("$.reasoning.effort").value("medium"))
			.andExpect(jsonPath("$.max_output_tokens").value(16000))
			.andExpect(jsonPath("$.store").value(false))
			.andExpect(jsonPath("$.text.format.type").value("json_schema"))
			.andExpect(jsonPath("$.text.format.name").value("answer_evaluation"))
			.andExpect(jsonPath("$.text.format.strict").value(true))
			.andExpect(jsonPath("$.text.format.schema.properties.dimensions.items.properties.code.enum[0]").value("TASK"))
			.andRespond(withSuccess(response(OUTPUT_JSON), MediaType.APPLICATION_JSON));

		EvaluationOutput output = model.evaluate(PROMPT);

		assertThat(output.dimensions()).hasSize(4);
		assertThat(output.dimensions().get(0).code()).isEqualTo(CompetencyDimension.TASK);
		assertThat(output.dimensions().get(2).level()).isEqualTo(OpicLevel.IL);
		assertThat(output.corrections().get(0).corrected()).isEqualTo("I went");
		assertThat(output.keyExpressions().get(0).expression()).isEqualTo("Looking back");
		assertThat(model.provider()).isEqualTo("openai");
		assertThat(model.model()).isEqualTo("gpt-5.6-sol");
		server.verify();
	}

	@Test
	void schemaFollowsStrictModeRulesEverywhere() {
		assertStrict(OpenAiAnswerEvaluationModel.SCHEMA);
	}

	@Test
	void refusalIsPermanent() throws Exception {
		String refusal = """
			{"status":"completed","output":[{"type":"message","role":"assistant",
			 "content":[{"type":"refusal","refusal":"I can't help with that."}]}]}
			""";
		server.expect(requestTo(properties.getOpenaiUrl())).andRespond(withSuccess(refusal, MediaType.APPLICATION_JSON));

		assertFailure("REFUSED", false);
	}

	@Test
	void truncatedOutputIsRetryable() {
		String incomplete = """
			{"status":"incomplete","incomplete_details":{"reason":"max_output_tokens"},"output":[]}
			""";
		server.expect(requestTo(properties.getOpenaiUrl())).andRespond(withSuccess(incomplete, MediaType.APPLICATION_JSON));

		assertFailure("TRUNCATED", true);
	}

	@Test
	void unparseableOutputIsRetryable() throws Exception {
		server.expect(requestTo(properties.getOpenaiUrl()))
			.andRespond(withSuccess(response("{not json"), MediaType.APPLICATION_JSON));

		assertFailure("INVALID_OUTPUT", true);
	}

	@Test
	void rateLimitsAndServerErrorsAreRetryable() {
		server.expect(requestTo(properties.getOpenaiUrl())).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		assertFailure("RATE_LIMITED", true);

		server.reset();
		server.expect(requestTo(properties.getOpenaiUrl())).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
		assertFailure("PROVIDER_ERROR", true);
	}

	@Test
	void badRequestsAndAuthErrorsArePermanent() {
		server.expect(requestTo(properties.getOpenaiUrl())).andRespond(withStatus(HttpStatus.BAD_REQUEST));
		assertFailure("BAD_REQUEST", false);

		server.reset();
		server.expect(requestTo(properties.getOpenaiUrl())).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
		assertFailure("NOT_AUTHORIZED", false);
	}

	@Test
	void missingApiKeyIsPermanentAndSendsNothing() {
		aiProperties.setOpenaiApiKey(" ");

		assertFailure("NOT_CONFIGURED", false);
		server.verify();
	}

	private void assertFailure(String reason, boolean retryable) {
		assertThatThrownBy(() -> model.evaluate(PROMPT))
			.isInstanceOfSatisfying(EvaluationFailure.class, e -> {
				assertThat(e.reason()).isEqualTo(reason);
				assertThat(e.retryable()).isEqualTo(retryable);
			});
	}

	/** A Responses API reply whose message carries {@code text} as output_text (with a reasoning item first). */
	private String response(String text) throws Exception {
		ObjectNode root = json.createObjectNode();
		root.put("status", "completed");
		root.putArray("output").addObject().put("type", "reasoning");
		ObjectNode message = ((com.fasterxml.jackson.databind.node.ArrayNode) root.get("output")).addObject();
		message.put("type", "message");
		message.put("role", "assistant");
		message.putArray("content").addObject().put("type", "output_text").put("text", text);
		return json.writeValueAsString(root);
	}

	private static void assertStrict(JsonNode schema) {
		if ("object".equals(schema.path("type").asText())) {
			assertThat(schema.path("additionalProperties").asBoolean(true)).as("additionalProperties").isFalse();
			JsonNode properties = schema.path("properties");
			assertThat(schema.path("required").size()).as("required lists every property").isEqualTo(properties.size());
			Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
			while (fields.hasNext()) {
				assertStrict(fields.next().getValue());
			}
		}
		if ("array".equals(schema.path("type").asText())) {
			assertStrict(schema.path("items"));
		}
	}
}
