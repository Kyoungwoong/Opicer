package com.opicer.api.goodanswer.infrastructure;

import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.shared.domain.OpicLevel;
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
	public List<GoodAnswerSample> findSimilar(
		UUID topicId,
		float[] embedding,
		int limit,
		String questionType,
		OpicLevel targetLevel
	) {
		PGobject vector = new PgVectorConverter().convertToDatabaseColumn(embedding);
		StringBuilder sql = new StringBuilder("""
			select *
			from good_answer_sample
			where topic_id = :topicId
			""");
		if (targetLevel != null) {
			sql.append(" and level = :targetLevel");
		}
		if (questionType != null && !questionType.isBlank()) {
			sql.append("""
				 and exists (
				 	select 1
				 	from good_answer_sample_tags t
				 	where t.sample_id = good_answer_sample.id
				 	and lower(t.tag) = lower(:questionType)
				 )
				""");
		}
		sql.append("""
			 order by embedding <-> :embedding
			 limit :limit
			""");
		Query query = entityManager.createNativeQuery(sql.toString(), GoodAnswerSample.class);
		query.setParameter("topicId", topicId);
		query.setParameter("embedding", vector);
		query.setParameter("limit", limit);
		if (targetLevel != null) {
			query.setParameter("targetLevel", targetLevel.name());
		}
		if (questionType != null && !questionType.isBlank()) {
			query.setParameter("questionType", questionType.trim());
		}
		@SuppressWarnings("unchecked")
		List<GoodAnswerSample> result = query.getResultList();
		return result;
	}
}
