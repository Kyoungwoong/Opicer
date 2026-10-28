package com.opicer.api.opic.application;

import com.opicer.api.opic.application.OpicScheduleCommandService.ScheduleInput;
import com.opicer.api.opic.domain.OpicExamSchedule;
import com.opicer.api.opic.infrastructure.OpicExamScheduleRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Admin schedule entry on real Postgres: date order, one row per date and round name, edits keep the source. */
@SpringBootTest
class OpicScheduleCommandServiceTest {

	private static final LocalDate EXAM = LocalDate.of(2026, 11, 8);

	@Autowired
	private OpicScheduleCommandService service;

	@Autowired
	private OpicExamScheduleRepository repository;

	@AfterEach
	void cleanUp() {
		repository.deleteAll();
	}

	@Test
	void createTrimsTheRoundNameAndRecordsAManualSource() {
		OpicExamSchedule saved = service.create(input(EXAM, "  정기 평가 "));

		OpicExamSchedule stored = repository.findById(saved.getId()).orElseThrow();
		assertThat(stored.getRoundName()).isEqualTo("정기 평가");
		assertThat(stored.getSource()).isEqualTo(OpicExamSchedule.SOURCE_MANUAL);
		assertThat(stored.getRegistrationEnd()).isEqualTo(EXAM.minusDays(5));
	}

	@Test
	void onlyTheExamDateIsRequiredAmongTheDates() {
		OpicExamSchedule saved = service.create(new ScheduleInput(EXAM, null, null, null, "정기 평가", false));

		assertThat(repository.findById(saved.getId())).get()
			.extracting(OpicExamSchedule::getRegistrationStart, OpicExamSchedule::getScoreReleaseDate)
			.containsOnlyNulls();
	}

	@Test
	void theSameDateAndRoundNameIsRejectedButAnotherRoundOnThatDayIsNot() {
		service.create(input(EXAM, "정기 평가"));
		service.create(input(EXAM, "특별 평가"));

		assertThatThrownBy(() -> service.create(input(EXAM, "정기 평가 ")))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.OPIC_SCHEDULE_DUPLICATE);
		assertThat(repository.count()).isEqualTo(2);
	}

	@Test
	void datesMustRunFromRegistrationToScoreRelease() {
		assertValidationError(new ScheduleInput(EXAM, EXAM.minusDays(3), EXAM.minusDays(10), null, "a", false));
		assertValidationError(new ScheduleInput(EXAM, null, EXAM.plusDays(1), null, "b", false));
		assertValidationError(new ScheduleInput(EXAM, EXAM.plusDays(1), null, null, "c", false));
		assertValidationError(new ScheduleInput(EXAM, null, null, EXAM.minusDays(1), "d", false));

		service.create(new ScheduleInput(EXAM, EXAM.minusDays(20), EXAM, EXAM, "same-day bounds", false));
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void updateChangesTheRoundButKeepsItsSourceAndRejectsAClash() {
		OpicExamSchedule imported = repository.save(new OpicExamSchedule(EXAM, null, null, null, "정기 평가", false,
			"partner-feed"));
		service.create(input(EXAM.plusDays(7), "정기 평가"));

		service.update(imported.getId(), new ScheduleInput(EXAM, EXAM.minusDays(14), EXAM.minusDays(4),
			EXAM.plusDays(5), "정기 평가 A", true));

		OpicExamSchedule stored = repository.findById(imported.getId()).orElseThrow();
		assertThat(stored.getRoundName()).isEqualTo("정기 평가 A");
		assertThat(stored.isEarlyRelease()).isTrue();
		assertThat(stored.getSource()).isEqualTo("partner-feed");

		assertThatThrownBy(() -> service.update(imported.getId(), input(EXAM.plusDays(7), "정기 평가")))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.OPIC_SCHEDULE_DUPLICATE);
		service.update(imported.getId(), input(EXAM, "정기 평가 A"));
	}

	@Test
	void deleteRemovesTheRowAndAMissingIdIsNotFound() {
		OpicExamSchedule saved = service.create(input(EXAM, "정기 평가"));

		service.delete(saved.getId());

		assertThat(repository.existsById(saved.getId())).isFalse();
		assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.OPIC_SCHEDULE_NOT_FOUND);
	}

	@Test
	void listingIsNewestExamFirst() {
		service.create(input(EXAM, "정기 평가"));
		service.create(input(EXAM.plusMonths(1), "정기 평가"));

		assertThat(service.findAll()).extracting(OpicExamSchedule::getExamDate)
			.containsExactly(EXAM.plusMonths(1), EXAM);
	}

	private void assertValidationError(ScheduleInput input) {
		assertThatThrownBy(() -> service.create(input))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	private static ScheduleInput input(LocalDate exam, String roundName) {
		return new ScheduleInput(exam, exam.minusDays(20), exam.minusDays(5), exam.plusDays(5), roundName, false);
	}
}
