package com.opicer.api.opic.application;

import com.opicer.api.opic.domain.OpicExamSchedule;
import com.opicer.api.opic.domain.OpicTestCenter;
import com.opicer.api.opic.infrastructure.OpicExamScheduleRepository;
import com.opicer.api.opic.infrastructure.OpicTestCenterRepository;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OpicQueryService {

	private final OpicExamScheduleRepository scheduleRepository;
	private final OpicTestCenterRepository centerRepository;

	public OpicQueryService(OpicExamScheduleRepository scheduleRepository, OpicTestCenterRepository centerRepository) {
		this.scheduleRepository = scheduleRepository;
		this.centerRepository = centerRepository;
	}

	public List<OpicExamSchedule> schedules(YearMonth month) {
		return scheduleRepository.findByExamDateBetweenOrderByExamDateAsc(month.atDay(1), month.atEndOfMonth());
	}

	/** Active centers; {@code region} (e.g. "서울") narrows the list when given. */
	public List<OpicTestCenter> centers(String region) {
		if (region == null || region.isBlank()) {
			return centerRepository.findByActiveTrueOrderByRegionNameAscNameAsc();
		}
		return centerRepository.findByActiveTrueAndRegionNameOrderByNameAsc(region.trim());
	}
}
