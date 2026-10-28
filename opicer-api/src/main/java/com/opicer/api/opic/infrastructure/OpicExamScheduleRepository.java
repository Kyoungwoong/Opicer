package com.opicer.api.opic.infrastructure;

import com.opicer.api.opic.domain.OpicExamSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpicExamScheduleRepository extends JpaRepository<OpicExamSchedule, UUID> {

	List<OpicExamSchedule> findByExamDateBetweenOrderByExamDateAscRoundNameAsc(LocalDate from, LocalDate to);
}
