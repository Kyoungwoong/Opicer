package com.opicer.api.goodanswer.application;

import com.opicer.api.ai.application.AiEmbeddingService;
import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.goodanswer.infrastructure.GoodAnswerSampleRepository;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoodAnswerSampleQueryService {

	private final GoodAnswerSampleRepository repository;
	private final AiEmbeddingService embeddingService;

	public GoodAnswerSampleQueryService(
		GoodAnswerSampleRepository repository,
		AiEmbeddingService embeddingService
	) {
		this.repository = repository;
		this.embeddingService = embeddingService;
	}

	@Transactional(readOnly = true)
	public List<GoodAnswerSample> listByTopic(UUID topicId) {
		return repository.findByTopicId(topicId);
	}

	@Transactional(readOnly = true)
	public List<GoodAnswerSample> findSimilar(
		UUID topicId,
		String transcript,
		int limit,
		String questionType,
		OpicLevel targetLevel
	) {
		try {
			float[] embedding = embeddingService.embed(transcript);
			List<GoodAnswerSample> filtered = repository.findSimilar(topicId, embedding, limit, questionType, targetLevel);
			if (!filtered.isEmpty() || (questionType == null && targetLevel == null)) {
				return filtered;
			}
			return repository.findSimilar(topicId, embedding, limit, null, null);
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			throw new ApiException(ErrorCode.AI_RAG_FAILED, e.getMessage());
		}
	}
}
