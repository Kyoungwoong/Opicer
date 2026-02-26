package com.opicer.api.ai.application;

import com.opicer.api.config.AiProperties;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AiEmbeddingService {

	private static final Logger log = LoggerFactory.getLogger(AiEmbeddingService.class);

	private final AiProperties aiProperties;
	private final RestClient restClient;

	public AiEmbeddingService(AiProperties aiProperties) {
		this.aiProperties = aiProperties;
		this.restClient = RestClient.create();
	}

	public float[] embed(String input) {
		String apiKey = aiProperties.getOpenaiApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw new ApiException(ErrorCode.AI_NOT_CONFIGURED);
		}
		try {
			Map<String, Object> body = Map.of(
				"model", aiProperties.getOpenaiEmbeddingModel(),
				"input", input
			);
			EmbeddingResponse response = restClient.post()
				.uri("https://api.openai.com/v1/embeddings")
				.header("Authorization", "Bearer " + apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(EmbeddingResponse.class);

			if (response == null || response.data() == null || response.data().isEmpty()) {
				throw new ApiException(ErrorCode.AI_EMBEDDING_FAILED, "Empty embedding response");
			}
			List<Double> vector = response.data().get(0).embedding();
			float[] out = new float[vector.size()];
			for (int i = 0; i < vector.size(); i++) {
				out[i] = vector.get(i).floatValue();
			}
			return out;
		} catch (ApiException e) {
			throw e;
		} catch (RestClientException e) {
			log.error("[Embedding] API call failed: {}", e.getMessage(), e);
			throw new ApiException(ErrorCode.AI_EMBEDDING_FAILED, e.getMessage());
		} catch (Exception e) {
			log.error("[Embedding] Unexpected error: {}", e.getMessage(), e);
			throw new ApiException(ErrorCode.AI_EMBEDDING_FAILED, e.getMessage());
		}
	}

	private record EmbeddingResponse(List<EmbeddingData> data) {}
	private record EmbeddingData(List<Double> embedding) {}
}
