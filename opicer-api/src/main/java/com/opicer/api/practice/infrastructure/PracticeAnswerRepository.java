package com.opicer.api.practice.infrastructure;

import com.opicer.api.practice.domain.PracticeAnswer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PracticeAnswerRepository extends JpaRepository<PracticeAnswer, UUID> {
	List<PracticeAnswer> findBySessionIdOrderByQuestionOrderAsc(UUID sessionId);

	/** Per-session answer counts without loading answer text. */
	@Query("""
		select a.sessionId as sessionId, count(a) as answerCount, count(a.analysis) as analyzedCount
		from PracticeAnswer a
		where a.sessionId in :sessionIds
		group by a.sessionId
		""")
	List<SessionAnswerCounts> countBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);

	interface SessionAnswerCounts {
		UUID getSessionId();
		long getAnswerCount();
		long getAnalyzedCount();
	}

	@Modifying
	@Query("delete from PracticeAnswer a where a.sessionId = :sessionId")
	int deleteBySessionId(@Param("sessionId") UUID sessionId);
}
