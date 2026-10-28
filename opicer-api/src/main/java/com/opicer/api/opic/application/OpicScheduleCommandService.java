package com.opicer.api.opic.application;

import com.opicer.api.opic.domain.OpicExamSchedule;
import com.opicer.api.opic.infrastructure.OpicExamScheduleRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-entered exam rounds for the schedule calendar. A round is one exam date plus a round name; the dates must run
 * registration start, registration end, exam date, score release, skipping any that are not known yet.
 */
@Service
public class OpicScheduleCommandService {

	private final OpicExamScheduleRepository repository;

	public OpicScheduleCommandService(OpicExamScheduleRepository repository) {
		this.repository = repository;
	}

	/** Newest exam date first. */
	@Transactional(readOnly = true)
	public List<OpicExamSchedule> findAll() {
		return repository.findAllByOrderByExamDateDescRoundNameAsc();
	}

	@Transactional
	public OpicExamSchedule create(ScheduleInput input) {
		input.validate();
		String roundName = input.roundName().trim();
		if (repository.existsByExamDateAndRoundName(input.examDate(), roundName)) {
			throw new ApiException(ErrorCode.OPIC_SCHEDULE_DUPLICATE);
		}
		return repository.save(new OpicExamSchedule(input.examDate(), input.registrationStart(), input.registrationEnd(),
			input.scoreReleaseDate(), roundName, input.earlyRelease(), OpicExamSchedule.SOURCE_MANUAL));
	}

	@Transactional
	public OpicExamSchedule update(UUID id, ScheduleInput input) {
		input.validate();
		OpicExamSchedule schedule = find(id);
		String roundName = input.roundName().trim();
		if (repository.existsByExamDateAndRoundNameAndIdNot(input.examDate(), roundName, id)) {
			throw new ApiException(ErrorCode.OPIC_SCHEDULE_DUPLICATE);
		}
		schedule.update(input.examDate(), input.registrationStart(), input.registrationEnd(), input.scoreReleaseDate(),
			roundName, input.earlyRelease());
		return schedule;
	}

	/** Nothing refers to a round, so it is removed outright. */
	@Transactional
	public void delete(UUID id) {
		repository.delete(find(id));
	}

	private OpicExamSchedule find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.OPIC_SCHEDULE_NOT_FOUND));
	}

	public record ScheduleInput(LocalDate examDate, LocalDate registrationStart, LocalDate registrationEnd,
		LocalDate scoreReleaseDate, String roundName, boolean earlyRelease) {

		void validate() {
			if (isAfter(registrationStart, registrationEnd)) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "registration must start before it ends");
			}
			if (isAfter(registrationStart, examDate) || isAfter(registrationEnd, examDate)) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "registration must close by the exam date");
			}
			if (isAfter(examDate, scoreReleaseDate)) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "scores cannot be released before the exam");
			}
		}

		private static boolean isAfter(LocalDate first, LocalDate second) {
			return first != null && second != null && first.isAfter(second);
		}
	}
}
