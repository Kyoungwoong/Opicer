package com.opicer.api.opic.application;

import com.opicer.api.opic.domain.OpicExamSchedule;
import com.opicer.api.opic.domain.OpicTestCenter;
import com.opicer.api.opic.infrastructure.OpicExamScheduleRepository;
import com.opicer.api.opic.infrastructure.OpicSiteParser.CenterRow;
import com.opicer.api.opic.infrastructure.OpicSiteParser.ScheduleRow;
import com.opicer.api.opic.infrastructure.OpicTestCenterRepository;
import java.time.Instant;
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

	/** Inserts new rounds and updates known ones by external key. Returns the number of rows written. */
	@Transactional
	public int upsertSchedules(List<ScheduleRow> rows, Instant syncedAt) {
		Map<String, ScheduleRow> byKey = rows.stream()
			.collect(Collectors.toMap(ScheduleRow::externalKey, Function.identity(), (a, b) -> b));
		Map<String, OpicExamSchedule> existing = scheduleRepository.findByExternalKeyIn(byKey.keySet()).stream()
			.collect(Collectors.toMap(OpicExamSchedule::getExternalKey, Function.identity()));

		for (ScheduleRow row : byKey.values()) {
			OpicExamSchedule schedule = existing.getOrDefault(row.externalKey(), new OpicExamSchedule(row.externalKey()));
			schedule.apply(row.examDate(), row.registrationStart(), row.registrationEnd(),
				row.scoreReleaseDate(), row.roundName(), row.earlyRelease(), syncedAt);
			scheduleRepository.save(schedule);
		}
		return byKey.size();
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

	public record CenterSyncResult(int upserted, int deactivated) {}
}
