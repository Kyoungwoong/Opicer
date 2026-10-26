package com.opicer.api.ai.infrastructure;

import com.opicer.api.ai.application.SpeechToText;
import com.opicer.api.config.AiProperties;
import com.opicer.api.config.SttProperties;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** OpenAI Whisper transcription. Active with {@code opicer.ai.stt.provider=openai}. */
@Component
@ConditionalOnProperty(name = SttProperties.PROVIDER_KEY, havingValue = "openai")
public class OpenAiWhisperSpeechToText implements SpeechToText {

	private static final Logger log = LoggerFactory.getLogger(OpenAiWhisperSpeechToText.class);

	private final AiProperties aiProperties;
	private final SttProperties sttProperties;
	private final RestClient restClient;

	public OpenAiWhisperSpeechToText(AiProperties aiProperties, SttProperties sttProperties, RestClient.Builder builder) {
		this.aiProperties = aiProperties;
		this.sttProperties = sttProperties;
		this.restClient = builder.build();
	}

	@Override
	public String transcribe(InputStreamSource audio, String filename, String hint) {
		String apiKey = aiProperties.getOpenaiApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw new ApiException(ErrorCode.AI_NOT_CONFIGURED);
		}
		try {
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", new NamedInputStreamResource(audio.getInputStream(), filename));
			body.add("model", sttProperties.getOpenaiModel());
			body.add("response_format", "text");

			return restClient.post()
				.uri(sttProperties.getOpenaiUrl())
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(body)
				.retrieve()
				.body(String.class);
		} catch (RestClientException | IOException e) {
			log.error("[STT:openai] transcription failed: {}", e.getMessage());
			throw new ApiException(ErrorCode.AI_TRANSCRIPTION_FAILED, e.getMessage());
		}
	}

	@Override
	public String provider() {
		return "openai";
	}

	/** Whisper infers the audio format from the part's filename. */
	private static class NamedInputStreamResource extends InputStreamResource {

		private final String filename;

		NamedInputStreamResource(InputStream in, String filename) {
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
