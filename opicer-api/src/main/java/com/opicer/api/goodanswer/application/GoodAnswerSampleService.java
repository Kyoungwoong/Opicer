package com.opicer.api.goodanswer.application;

import com.opicer.api.ai.application.AiEmbeddingService;
import com.opicer.api.ai.application.AiTranscriptionService;
import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.goodanswer.infrastructure.GoodAnswerSampleRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.infrastructure.AudioStorage;
import com.opicer.api.topic.application.TopicService;
import com.opicer.api.topic.domain.Topic;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class GoodAnswerSampleService {

	private final GoodAnswerSampleRepository repository;
	private final TopicService topicService;
	private final AiEmbeddingService embeddingService;
	private final AudioStorage audioStorage;
	private final AiTranscriptionService transcriptionService;

	public GoodAnswerSampleService(
		GoodAnswerSampleRepository repository,
		TopicService topicService,
		AiEmbeddingService embeddingService,
		AudioStorage audioStorage,
		AiTranscriptionService transcriptionService
	) {
		this.repository = repository;
		this.topicService = topicService;
		this.embeddingService = embeddingService;
		this.audioStorage = audioStorage;
		this.transcriptionService = transcriptionService;
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

	@Transactional
	public List<GoodAnswerSample> createFromAudio(UUID topicId, OpicLevel level, List<MultipartFile> audios,
		String summary, List<String> tags, List<String> keyExpressions) {
		if (audios == null || audios.isEmpty()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "Audio file is required");
		}
		return audios.stream()
			.map(audio -> {
				String audioUrl = audioStorage.save(audio);
				String transcript = transcriptionService.transcribe(audio);
				if (transcript == null || transcript.isBlank()) {
					throw new ApiException(ErrorCode.AI_TRANSCRIPTION_FAILED, "Empty transcript");
				}
				return create(topicId, level, transcript.trim(), audioUrl, summary, tags, keyExpressions);
			})
			.toList();
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
