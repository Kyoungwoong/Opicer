package com.opicer.api.opic.application;

import com.opicer.api.opic.domain.OpicExamSchedule;
import com.opicer.api.opic.infrastructure.OpicExamScheduleRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** A month's schedule on real Postgres: only that month's exam dates, by date then round name. */
@SpringBootTest
class OpicScheduleQueryTest {

	@Autowired
	private OpicQueryService queryService;

	@Autowired
	private OpicExamScheduleRepository repository;

	@AfterEach
	void cleanUp() {
		repository.deleteAll();
	}

	@Test
	void aMonthHoldsOnlyItsOwnExamDatesInOrder() {
		repository.saveAll(List.of(
			round(LocalDate.of(2026, 10, 31), "261031 정기 평가"),
			round(LocalDate.of(2026, 11, 30), "261130 정기 평가"),
			round(LocalDate.of(2026, 11, 1), "261101 정기 평가 B"),
			round(LocalDate.of(2026, 11, 1), "261101 정기 평가 A"),
			round(LocalDate.of(2026, 12, 1), "261201 정기 평가")));

		List<OpicExamSchedule> november = queryService.schedules(YearMonth.of(2026, 11));

		assertThat(november).extracting(OpicExamSchedule::getRoundName)
			.containsExactly("261101 정기 평가 A", "261101 정기 평가 B", "261130 정기 평가");
		assertThat(november).allMatch(s -> OpicExamSchedule.SOURCE_MANUAL.equals(s.getSource()) && s.getCreatedAt() != null);
	}

	@Test
	void aMonthWithoutRoundsIsEmpty() {
		repository.save(round(LocalDate.of(2026, 11, 8), "261108 정기 평가"));

		assertThat(queryService.schedules(YearMonth.of(2027, 1))).isEmpty();
	}

	private static OpicExamSchedule round(LocalDate examDate, String name) {
		return new OpicExamSchedule(examDate, examDate.minusDays(30), examDate.minusDays(7), examDate.plusDays(5), name,
			false, OpicExamSchedule.SOURCE_MANUAL);
	}
}
