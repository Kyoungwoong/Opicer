package com.opicer.api.question.infrastructure;

import com.opicer.api.question.domain.Question;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
	java.util.List<Question> findByActiveTrueAndTopicOrderByCreatedAtAscIdAsc(String topic);
}
