package com.opicer.api.dailysentence.infrastructure;

import com.opicer.api.dailysentence.domain.DailySentence;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailySentenceRepository extends JpaRepository<DailySentence, UUID> {

	Optional<DailySentence> findByDate(LocalDate date);

	boolean existsByDate(LocalDate date);

	boolean existsByDateAndIdNot(LocalDate date, UUID id);
}
