package com.opicer.api.practice.infrastructure;

import com.opicer.api.practice.domain.PracticeCreditCharge;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeCreditChargeRepository extends JpaRepository<PracticeCreditCharge, UUID> {
	Optional<PracticeCreditCharge> findByTopicSelectionId(UUID topicSelectionId);
	long countByTopicSelectionId(UUID topicSelectionId);
}
