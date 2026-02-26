package com.opicer.api.ai.application;

import com.opicer.api.config.AiProperties;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.io.InputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AiTranscriptionService {

	private static final Logger log = LoggerFactory.getLogger(AiTranscriptionService.class);

	private final AiProperties aiProperties;
	private final RestClient restClient;

	public AiTranscriptionService(AiProperties aiProperties) {
		this.aiProperties = aiProperties;
		this.restClient = RestClient.create();
	}

	public String transcribe(MultipartFile audio) {
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
