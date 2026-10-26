package com.opicer.api.opic.application;

import com.opicer.api.opic.domain.OpicExamSchedule;
import com.opicer.api.opic.domain.OpicTestCenter;
import com.opicer.api.opic.infrastructure.OpicExamScheduleRepository;
import com.opicer.api.opic.infrastructure.OpicSiteParser.CenterRow;
import com.opicer.api.opic.infrastructure.OpicSiteParser.ScheduleRow;
import com.opicer.api.opic.infrastructure.OpicTestCenterRepository;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** DB side of the sync, kept separate so no transaction is held open during HTTP calls. */
@Service
public class OpicSyncWriter {

	private final OpicExamScheduleRepository scheduleRepository;
	private final OpicTestCenterRepository centerRepository;

	public OpicSyncWriter(OpicExamScheduleRepository scheduleRepository, OpicTestCenterRepository centerRepository) {
		this.scheduleRepository = scheduleRepository;
		this.centerRepository = centerRepository;
	}

	/**
	 * Upserts the fetched rounds by external key, then removes rounds that disappeared from the
	 * source within each fetched month. A month whose fetch came back empty is left untouched
	 * (it may be a maintenance response); months that were not fetched are never pruned.
	 */
	@Transactional
	public ScheduleSyncResult upsertSchedules(Map<YearMonth, List<ScheduleRow>> rowsByMonth, Instant syncedAt) {
		Map<String, ScheduleRow> byKey = rowsByMonth.values().stream()
			.flatMap(List::stream)
			.collect(Collectors.toMap(ScheduleRow::externalKey, Function.identity(), (a, b) -> b));
		Map<String, OpicExamSchedule> existing = scheduleRepository.findByExternalKeyIn(byKey.keySet()).stream()
			.collect(Collectors.toMap(OpicExamSchedule::getExternalKey, Function.identity()));

		for (ScheduleRow row : byKey.values()) {
			OpicExamSchedule schedule = existing.getOrDefault(row.externalKey(), new OpicExamSchedule(row.externalKey()));
			schedule.apply(row.examDate(), row.registrationStart(), row.registrationEnd(),
				row.scoreReleaseDate(), row.roundName(), row.earlyRelease(), syncedAt);
			scheduleRepository.save(schedule);
		}

		int removed = 0;
		for (Map.Entry<YearMonth, List<ScheduleRow>> month : rowsByMonth.entrySet()) {
			if (month.getValue().isEmpty()) {
				continue;
			}
			YearMonth ym = month.getKey();
			for (OpicExamSchedule stale : scheduleRepository.findByExamDateBetweenOrderByExamDateAsc(ym.atDay(1), ym.atEndOfMonth())) {
				if (!byKey.containsKey(stale.getExternalKey())) {
					scheduleRepository.delete(stale);
					removed++;
				}
			}
		}
		return new ScheduleSyncResult(byKey.size(), removed);
	}

	/**
	 * Upserts centers and deactivates those missing from {@code rows}.
	 * An empty list is treated as a failed fetch and changes nothing.
	 */
	@Transactional
	public CenterSyncResult upsertCenters(List<CenterRow> rows, Instant syncedAt) {
		if (rows.isEmpty()) {
			return new CenterSyncResult(0, 0);
		}
		Map<String, CenterRow> byKey = rows.stream()
			.collect(Collectors.toMap(CenterRow::externalKey, Function.identity(), (a, b) -> b));
		List<OpicTestCenter> all = centerRepository.findAll();
		Map<String, OpicTestCenter> existing = all.stream()
			.collect(Collectors.toMap(OpicTestCenter::getExternalKey, Function.identity()));

		for (CenterRow row : byKey.values()) {
			OpicTestCenter center = existing.getOrDefault(row.externalKey(), new OpicTestCenter(row.externalKey()));
			center.apply(row.name(), row.address(), row.latitude(), row.longitude(), row.phone(),
				row.subwayLines(), row.directions(), row.regionCode(), row.regionName(), syncedAt);
			centerRepository.save(center);
		}

		Set<String> seen = byKey.keySet();
		int deactivated = 0;
		for (OpicTestCenter center : all) {
			if (center.isActive() && !seen.contains(center.getExternalKey())) {
				center.deactivate();
				deactivated++;
			}
		}
		return new CenterSyncResult(byKey.size(), deactivated);
	}

	public record ScheduleSyncResult(int upserted, int removed) {}

	public record CenterSyncResult(int upserted, int deactivated) {}
}
