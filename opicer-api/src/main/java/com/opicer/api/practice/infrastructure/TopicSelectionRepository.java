package com.opicer.api.practice.infrastructure;

import com.opicer.api.practice.domain.TopicSelection;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicSelectionRepository extends JpaRepository<TopicSelection, UUID> {
	Optional<TopicSelection> findByIdAndUserId(UUID id, UUID userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from TopicSelection t where t.id = :id and t.userId = :userId")
	Optional<TopicSelection> findLockedByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
