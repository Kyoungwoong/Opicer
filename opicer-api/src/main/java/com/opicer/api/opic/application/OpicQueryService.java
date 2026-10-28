package com.opicer.api.opic.application;

import com.opicer.api.opic.domain.OpicExamSchedule;
import com.opicer.api.opic.domain.OpicTestCenter;
import com.opicer.api.opic.infrastructure.OpicCenterReviewRepository;
import com.opicer.api.opic.infrastructure.OpicCenterReviewRepository.ReviewStats;
import com.opicer.api.opic.infrastructure.OpicExamScheduleRepository;
import com.opicer.api.opic.infrastructure.OpicTestCenterRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OpicQueryService {

	private final OpicTestCenterRepository centerRepository;
	private final OpicCenterReviewRepository reviewRepository;
	private final OpicExamScheduleRepository scheduleRepository;

	public OpicQueryService(
		OpicTestCenterRepository centerRepository,
		OpicCenterReviewRepository reviewRepository,
		OpicExamScheduleRepository scheduleRepository
	) {
		this.centerRepository = centerRepository;
		this.reviewRepository = reviewRepository;
		this.scheduleRepository = scheduleRepository;
	}

	/** Exam rounds whose exam date falls in {@code month}, by date then round name. */
	public List<OpicExamSchedule> schedules(YearMonth month) {
		return scheduleRepository.findByExamDateBetweenOrderByExamDateAscRoundNameAsc(month.atDay(1), month.atEndOfMonth());
	}

	/** Active centers with their visible-review stats; {@code region} (e.g. "서울") narrows the list when given. */
	public List<CenterSummary> centers(String region) {
		List<OpicTestCenter> centers = region == null || region.isBlank()
			? centerRepository.findByActiveTrueOrderByRegionNameAscNameAsc()
			: centerRepository.findByActiveTrueAndRegionNameOrderByNameAsc(region.trim());
		Map<UUID, ReviewStats> stats = reviewRepository.findVisibleStats().stream()
			.collect(Collectors.toMap(ReviewStats::getCenterId, Function.identity()));
		return centers.stream().map(center -> CenterSummary.of(center, stats.get(center.getId()))).toList();
	}

	/** {@code averageRating} is rounded to one decimal and null when the center has no visible reviews. */
	public record CenterSummary(OpicTestCenter center, long reviewCount, Double averageRating) {
		static CenterSummary of(OpicTestCenter center, ReviewStats stats) {
			if (stats == null) {
				return new CenterSummary(center, 0, null);
			}
			return new CenterSummary(center, stats.getReviewCount(), Math.round(stats.getAverageRating() * 10) / 10.0);
		}
	}
}
