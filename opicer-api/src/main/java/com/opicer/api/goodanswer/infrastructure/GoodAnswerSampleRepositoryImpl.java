package com.opicer.api.goodanswer.infrastructure;

import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.shared.infrastructure.PgVectorConverter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class GoodAnswerSampleRepositoryImpl implements GoodAnswerSampleRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@code <=>} is pgvector's cosine distance (1 − cosine similarity). No ANN index on purpose: the topic
	 * filter keeps the scan to one topic's samples, so exact search is cheap and has full recall.
	 */
	@Override
	public List<GoodAnswerSample> findNearest(UUID topicId, float[] embedding, int limit) {
		Query query = entityManager.createNativeQuery("""
			select *
			from good_answer_sample
			where topic_id = :topicId
			  and embedding is not null
			order by embedding <=> cast(:embedding as vector), id
			limit :limit
			""", GoodAnswerSample.class);
		query.setParameter("topicId", topicId);
		query.setParameter("embedding", PgVectorConverter.toLiteral(embedding));
		query.setParameter("limit", limit);
		@SuppressWarnings("unchecked")
		List<GoodAnswerSample> result = query.getResultList();
		return result;
	}
}
