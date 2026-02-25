package com.opicer.api.practice.infrastructure;

import com.opicer.api.practice.domain.TopicSelection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicSelectionRepository extends JpaRepository<TopicSelection, UUID> {
}
