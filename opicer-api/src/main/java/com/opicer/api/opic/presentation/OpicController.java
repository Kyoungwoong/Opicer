package com.opicer.api.opic.presentation;

import com.opicer.api.opic.application.OpicCenterReviewQueryService;
import com.opicer.api.opic.application.OpicQueryService;
import com.opicer.api.opic.application.OpicQueryService.CenterSummary;
import com.opicer.api.opic.domain.OpicCenterReview;
import com.opicer.api.opic.domain.OpicExamSchedule;
import com.opicer.api.opic.domain.OpicTestCenter;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.shared.presentation.ApiResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only OPIc exam schedules, test centers (entered by admins) and their reviews. */
@RestController
@RequestMapping("/api/opic")
public class OpicController {

	private final OpicQueryService opicQueryService;
	private final OpicCenterReviewQueryService reviewQueryService;

	public OpicController(OpicQueryService opicQueryService, OpicCenterReviewQueryService reviewQueryService) {
		this.opicQueryService = opicQueryService;
		this.reviewQueryService = reviewQueryService;
	}

	/** {@code month} is YYYY-MM; defaults to the current month in Asia/Seoul. */
	@GetMapping("/schedules")
	public ResponseEntity<ApiResponse<ScheduleMonthResponse>> schedules(@RequestParam(required = false) String month) {
		YearMonth target = parseMonth(month);
		List<ScheduleResponse> items = opicQueryService.schedules(target).stream().map(ScheduleResponse::from).toList();
		return ResponseEntity.ok(ApiResponse.ok("OPIC_SCHEDULES", new ScheduleMonthResponse(target.toString(), items)));
	}

	@GetMapping("/centers")
	public ResponseEntity<ApiResponse<List<CenterResponse>>> centers(@RequestParam(required = false) String region) {
		return ResponseEntity.ok(ApiResponse.ok("OPIC_CENTERS",
			opicQueryService.centers(region).stream().map(CenterResponse::from).toList()));
	}

	/** Visible, admin-entered reviews of one center, newest first. */
	@GetMapping("/centers/{centerId}/reviews")
	public ResponseEntity<ApiResponse<List<ReviewResponse>>> reviews(@PathVariable UUID centerId) {
		return ResponseEntity.ok(ApiResponse.ok("OPIC_CENTER_REVIEWS",
			reviewQueryService.visibleReviews(centerId).stream().map(ReviewResponse::from).toList()));
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

	public record ScheduleMonthResponse(String month, List<ScheduleResponse> schedules) {}

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
		String regionName,
		long reviewCount,
		Double averageRating
	) {
		static CenterResponse from(CenterSummary summary) {
			OpicTestCenter c = summary.center();
			return new CenterResponse(c.getId(), c.getName(), c.getAddress(), c.getLatitude(), c.getLongitude(),
				c.getPhone(), c.getSubwayLines(), c.getDirections(), c.getRegionName(),
				summary.reviewCount(), summary.averageRating());
		}
	}

	public record ReviewResponse(UUID id, int rating, String content, String sourceLabel, Instant createdAt) {
		static ReviewResponse from(OpicCenterReview r) {
			return new ReviewResponse(r.getId(), r.getRating(), r.getContent(), r.getSourceLabel(), r.getCreatedAt());
		}
	}
}
