package com.opicer.api.ai.infrastructure;

import com.opicer.api.config.AiProperties;
import com.opicer.api.config.SttProperties;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Contract test against a fake HTTP server; no real OpenAI call. */
class OpenAiWhisperSpeechToTextTest {

	private static final InputStreamSource AUDIO = () -> new ByteArrayInputStream("fake-webm".getBytes());

	private final AiProperties aiProperties = new AiProperties();
	private final SttProperties sttProperties = new SttProperties();
	private MockRestServiceServer server;
	private OpenAiWhisperSpeechToText stt;

	@BeforeEach
	void setUp() {
		aiProperties.setOpenaiApiKey("test-key");
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		stt = new OpenAiWhisperSpeechToText(aiProperties, sttProperties, builder);
	}

	@Test
	void postsMultipartAudioWithModelAndReturnsTheText() {
		server.expect(requestTo(sttProperties.getOpenaiUrl()))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("Authorization", "Bearer test-key"))
			.andExpect(header("Content-Type", startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)))
			.andExpect(content().string(containsString("whisper-1")))
			.andExpect(content().string(containsString("filename=\"audio.webm\"")))
			.andRespond(withSuccess("I live in Seoul.", MediaType.TEXT_PLAIN));

		assertThat(stt.transcribe(AUDIO, "audio.webm", "Where do you live?")).isEqualTo("I live in Seoul.");
		assertThat(stt.provider()).isEqualTo("openai");
		server.verify();
	}

	@Test
	void missingApiKeyIsNotConfigured() {
		aiProperties.setOpenaiApiKey("");

		assertThatThrownBy(() -> stt.transcribe(AUDIO, "audio.webm", null))
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_NOT_CONFIGURED));
	}

	@Test
	void providerErrorBecomesTranscriptionFailed() {
		server.expect(requestTo(sttProperties.getOpenaiUrl())).andRespond(withServerError());

		assertThatThrownBy(() -> stt.transcribe(AUDIO, "audio.webm", null))
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_TRANSCRIPTION_FAILED));
	}
}
