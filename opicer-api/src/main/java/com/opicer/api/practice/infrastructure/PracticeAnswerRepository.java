package com.opicer.api.practice.infrastructure;

import com.opicer.api.practice.domain.PracticeAnswer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PracticeAnswerRepository extends JpaRepository<PracticeAnswer, UUID> {
	List<PracticeAnswer> findBySessionIdOrderByQuestionOrderAsc(UUID sessionId);

	List<PracticeAnswer> findBySessionIdIn(Collection<UUID> sessionIds);

	Optional<PracticeAnswer> findBySessionIdAndQuestionId(UUID sessionId, UUID questionId);

	@Modifying
	@Query("delete from PracticeAnswer a where a.sessionId = :sessionId")
	int deleteBySessionId(@Param("sessionId") UUID sessionId);
}
