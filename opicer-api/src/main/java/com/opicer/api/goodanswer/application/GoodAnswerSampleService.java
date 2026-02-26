package com.opicer.api.goodanswer.application;

import com.opicer.api.ai.application.AiEmbeddingService;
import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.goodanswer.infrastructure.GoodAnswerSampleRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.topic.application.TopicService;
import com.opicer.api.topic.domain.Topic;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoodAnswerSampleService {

	private final GoodAnswerSampleRepository repository;
	private final TopicService topicService;
	private final AiEmbeddingService embeddingService;

	public GoodAnswerSampleService(
		GoodAnswerSampleRepository repository,
		TopicService topicService,
		AiEmbeddingService embeddingService
	) {
		this.repository = repository;
		this.topicService = topicService;
		this.embeddingService = embeddingService;
	}

	@Transactional
	public GoodAnswerSample create(UUID topicId, OpicLevel level, String sampleText, String sampleAudioUrl,
		String summary, List<String> tags, List<String> keyExpressions) {
		Topic topic = topicService.getActiveOrThrow(topicId);
		float[] embedding = embeddingService.embed(sampleText);
		GoodAnswerSample sample = new GoodAnswerSample(
			topic, level, sampleText, sampleAudioUrl, summary, tags, keyExpressions, embedding
		);
		return repository.save(sample);
	}

	@Transactional(readOnly = true)
	public List<GoodAnswerSample> listByTopic(UUID topicId) {
		return repository.findByTopicId(topicId);
	}

	@Transactional
	public void delete(UUID id) {
		repository.deleteById(id);
	}

	@Transactional(readOnly = true)
	public List<GoodAnswerSample> findSimilar(UUID topicId, String transcript, int limit) {
		try {
			float[] embedding = embeddingService.embed(transcript);
			return repository.findSimilar(topicId, embedding, limit);
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			throw new ApiException(ErrorCode.AI_RAG_FAILED, e.getMessage());
		}
	}
}
