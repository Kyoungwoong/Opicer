package com.opicer.api.goodanswer.application;

import com.opicer.api.ai.application.AiEmbeddingService;
import com.opicer.api.config.RagProperties;
import com.opicer.api.goodanswer.application.SimilarityRanker.Candidate;
import com.opicer.api.goodanswer.application.SimilarityRanker.Ranked;
import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.goodanswer.infrastructure.GoodAnswerSampleRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GoodAnswerSampleQueryService {

	private static final Logger log = LoggerFactory.getLogger(GoodAnswerSampleQueryService.class);

	private final GoodAnswerSampleRepository repository;
	private final AiEmbeddingService embeddingService;
	private final RagProperties ragProperties;
	private final TransactionTemplate readOnlyTx;

	public GoodAnswerSampleQueryService(
		GoodAnswerSampleRepository repository,
		AiEmbeddingService embeddingService,
		RagProperties ragProperties,
		PlatformTransactionManager transactionManager
	) {
		this.repository = repository;
		this.embeddingService = embeddingService;
		this.ragProperties = ragProperties;
		this.readOnlyTx = new TransactionTemplate(transactionManager);
		this.readOnlyTx.setReadOnly(true);
	}

	@Transactional(readOnly = true)
	public List<GoodAnswerSample> listByTopic(UUID topicId) {
		return repository.findByTopicId(topicId);
	}

	/**
	 * Reference samples for a learner's answer: cosine nearest neighbours within the topic, then a similarity
	 * floor and MMR for diversity (docs/adr/0001-vector-similarity.md). Returns an empty list when the
	 * transcript is too short to compare. The embedding HTTP call runs outside any DB transaction.
	 */
	public List<ReferenceSample> findReferenceSamples(UUID topicId, String transcript) {
		if (wordCount(transcript) < ragProperties.getMinQueryWords()) {
			return List.of();
		}
		try {
			float[] query = embeddingService.embed(transcript);
			List<ReferenceSample> picked = readOnlyTx.execute(status -> rank(topicId, query));
			log.info("[RAG] topic={} picked={} similarities={}", topicId, picked.size(),
				picked.stream().map(s -> String.format("%.3f", s.similarity())).toList());
			return picked;
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			throw new ApiException(ErrorCode.AI_RAG_FAILED, e.getMessage());
		}
	}

	private List<ReferenceSample> rank(UUID topicId, float[] query) {
		List<Candidate<GoodAnswerSample>> candidates = repository
			.findNearest(topicId, query, ragProperties.getCandidates())
			.stream()
			.map(sample -> new Candidate<>(sample.getId().toString(), sample.getEmbedding(), sample))
			.toList();
		List<Ranked<GoodAnswerSample>> ranked = SimilarityRanker.select(
			query, candidates, ragProperties.getTopK(), ragProperties.getMinSimilarity(), ragProperties.getMmrLambda());
		// Map inside the transaction so the lazy key-expression collection is loaded.
		return ranked.stream().map(r -> {
			GoodAnswerSample sample = r.item();
			return new ReferenceSample(sample.getId(), sample.getLevel(), sample.getSummary(),
				List.copyOf(sample.getKeyExpressions()), sample.getSampleText(), r.similarity());
		}).toList();
	}

	private static int wordCount(String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}
		return text.trim().split("\\s+").length;
	}
}
