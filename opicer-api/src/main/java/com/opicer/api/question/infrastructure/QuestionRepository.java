package com.opicer.api.question.infrastructure;

import com.opicer.api.question.domain.Question;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
	java.util.List<Question> findByActiveTrueAndTopicOrderByCreatedAtAscIdAsc(String topic);

	/** The whole active bank in insertion order; small enough to compose a mock test in memory. */
	java.util.List<Question> findByActiveTrueOrderByCreatedAtAscIdAsc();

	boolean existsByTopicAndPromptText(String topic, String promptText);

	boolean existsByTopic(String topic);

	/** Moves every question of a renamed topic to its new title; questions are linked to topics by title. */
	@Modifying
	@Query("update Question q set q.topic = :to where q.topic = :from")
	int renameTopic(@Param("from") String from, @Param("to") String to);
}
