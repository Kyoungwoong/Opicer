package com.opicer.api.opic.presentation;

import com.opicer.api.opic.application.OpicScheduleCommandService;
import com.opicer.api.opic.application.OpicScheduleCommandService.ScheduleInput;
import com.opicer.api.opic.domain.OpicExamSchedule;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-entered OPIc exam rounds behind the schedule calendar (ROLE_ADMIN via the /api/admin/** rule). */
@RestController
@RequestMapping("/api/admin/opic-schedules")
public class AdminOpicScheduleController {

	private final OpicScheduleCommandService commandService;

	public AdminOpicScheduleController(OpicScheduleCommandService commandService) {
		this.commandService = commandService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ScheduleResponse>>> list() {
		return ResponseEntity.ok(ApiResponse.ok("OPIC_SCHEDULE_LIST_OK",
			commandService.findAll().stream().map(ScheduleResponse::from).toList()));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ScheduleResponse>> create(@Valid @RequestBody ScheduleRequest request) {
		return ResponseEntity.status(201).body(ApiResponse.created("OPIC_SCHEDULE_CREATED",
			ScheduleResponse.from(commandService.create(request.toInput()))));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<ScheduleResponse>> update(@PathVariable UUID id,
		@Valid @RequestBody ScheduleRequest request) {
		return ResponseEntity.ok(ApiResponse.ok("OPIC_SCHEDULE_UPDATED",
			ScheduleResponse.from(commandService.update(id, request.toInput()))));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
		commandService.delete(id);
		return ResponseEntity.ok(ApiResponse.ok("OPIC_SCHEDULE_DELETED", null));
	}

	public record ScheduleRequest(
		@NotNull LocalDate examDate,
		LocalDate registrationStart,
		LocalDate registrationEnd,
		LocalDate scoreReleaseDate,
		@NotBlank @Size(max = 255) String roundName,
		boolean earlyRelease
	) {
		ScheduleInput toInput() {
			return new ScheduleInput(examDate, registrationStart, registrationEnd, scoreReleaseDate, roundName,
				earlyRelease);
		}
	}

	public record ScheduleResponse(
		UUID id,
		LocalDate examDate,
		LocalDate registrationStart,
		LocalDate registrationEnd,
		LocalDate scoreReleaseDate,
		String roundName,
		boolean earlyRelease,
		String source,
		Instant updatedAt
	) {
		static ScheduleResponse from(OpicExamSchedule s) {
			return new ScheduleResponse(s.getId(), s.getExamDate(), s.getRegistrationStart(), s.getRegistrationEnd(),
				s.getScoreReleaseDate(), s.getRoundName(), s.isEarlyRelease(), s.getSource(), s.getUpdatedAt());
		}
	}
}
