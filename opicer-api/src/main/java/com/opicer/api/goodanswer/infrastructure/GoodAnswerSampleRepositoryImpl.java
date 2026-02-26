package com.opicer.api.goodanswer.infrastructure;

import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.shared.infrastructure.PgVectorConverter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.UUID;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Repository;

@Repository
public class GoodAnswerSampleRepositoryImpl implements GoodAnswerSampleRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<GoodAnswerSample> findSimilar(UUID topicId, float[] embedding, int limit) {
		PGobject vector = new PgVectorConverter().convertToDatabaseColumn(embedding);
		String sql = """
			select *
			from good_answer_sample
			where topic_id = :topicId
			order by embedding <-> :embedding
			limit :limit
			""";
		Query query = entityManager.createNativeQuery(sql, GoodAnswerSample.class);
		query.setParameter("topicId", topicId);
		query.setParameter("embedding", vector);
		query.setParameter("limit", limit);
		@SuppressWarnings("unchecked")
		List<GoodAnswerSample> result = query.getResultList();
		return result;
	}
}
