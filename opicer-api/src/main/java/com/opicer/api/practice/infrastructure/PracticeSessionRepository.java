package com.opicer.api.practice.infrastructure;

import com.opicer.api.practice.domain.PracticeSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeSessionRepository extends JpaRepository<PracticeSession, UUID> {
	Optional<PracticeSession> findByTopicSelectionId(UUID topicSelectionId);

	Optional<PracticeSession> findByIdAndUserId(UUID id, UUID userId);

	List<PracticeSession> findByUserIdOrderByCompletedAtDesc(UUID userId, Pageable pageable);
}
