package com.opicer.api.opic.presentation;

import com.opicer.api.opic.application.OpicQueryService;
import com.opicer.api.opic.domain.OpicExamSchedule;
import com.opicer.api.opic.domain.OpicTestCenter;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.presentation.ApiResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only OPIc facts synced from opic.or.kr. */
@RestController
@RequestMapping("/api/opic")
public class OpicController {

	public static final String SOURCE = "opic.or.kr";

	private final OpicQueryService opicQueryService;

	public OpicController(OpicQueryService opicQueryService) {
		this.opicQueryService = opicQueryService;
	}

	/** {@code month} is YYYY-MM; defaults to the current month in Asia/Seoul. */
	@GetMapping("/schedules")
	public ResponseEntity<ApiResponse<ScheduleMonthResponse>> schedules(@RequestParam(required = false) String month) {
		YearMonth target = parseMonth(month);
		List<ScheduleResponse> items = opicQueryService.schedules(target).stream().map(ScheduleResponse::from).toList();
		return ResponseEntity.ok(ApiResponse.ok("OPIC_SCHEDULES", new ScheduleMonthResponse(target.toString(), SOURCE, items)));
	}

	@GetMapping("/centers")
	public ResponseEntity<ApiResponse<List<CenterResponse>>> centers(@RequestParam(required = false) String region) {
		return ResponseEntity.ok(ApiResponse.ok("OPIC_CENTERS",
			opicQueryService.centers(region).stream().map(CenterResponse::from).toList()));
	}

	private static YearMonth parseMonth(String month) {
		if (month == null || month.isBlank()) {
			return YearMonth.now(ZoneId.of("Asia/Seoul"));
		}
		try {
			return YearMonth.parse(month.trim());
		} catch (DateTimeParseException e) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "month must be YYYY-MM");
		}
	}

	public record ScheduleMonthResponse(String month, String source, List<ScheduleResponse> schedules) {}

	public record ScheduleResponse(
		LocalDate examDate,
		LocalDate registrationStart,
		LocalDate registrationEnd,
		LocalDate scoreReleaseDate,
		String roundName,
		boolean earlyRelease
	) {
		static ScheduleResponse from(OpicExamSchedule s) {
			return new ScheduleResponse(s.getExamDate(), s.getRegistrationStart(), s.getRegistrationEnd(),
				s.getScoreReleaseDate(), s.getRoundName(), s.isEarlyRelease());
		}
	}

	public record CenterResponse(
		UUID id,
		String name,
		String address,
		Double latitude,
		Double longitude,
		String phone,
		String subwayLines,
		String directions,
		String regionName
	) {
		static CenterResponse from(OpicTestCenter c) {
			return new CenterResponse(c.getId(), c.getName(), c.getAddress(), c.getLatitude(), c.getLongitude(),
				c.getPhone(), c.getSubwayLines(), c.getDirections(), c.getRegionName());
		}
	}
}
