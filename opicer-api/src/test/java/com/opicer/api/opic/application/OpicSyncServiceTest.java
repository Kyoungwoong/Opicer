package com.opicer.api.opic.application;

import com.opicer.api.opic.domain.OpicTestCenter;
import com.opicer.api.opic.infrastructure.OpicExamScheduleRepository;
import com.opicer.api.opic.infrastructure.OpicSiteClient;
import com.opicer.api.opic.infrastructure.OpicSiteParser.CenterRow;
import com.opicer.api.opic.infrastructure.OpicSiteParser.ScheduleRow;
import com.opicer.api.opic.infrastructure.OpicTestCenterRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class OpicSyncServiceTest {

	private static final YearMonth NEXT_MONTH = YearMonth.now(ZoneId.of("Asia/Seoul")).plusMonths(1);

	@Autowired
	private OpicSyncService syncService;

	@Autowired
	private OpicQueryService queryService;

	@Autowired
	private OpicExamScheduleRepository scheduleRepository;

	@Autowired
	private OpicTestCenterRepository centerRepository;

	@MockBean
	private OpicSiteClient client;

	@BeforeEach
	void setUp() {
		scheduleRepository.deleteAll();
		centerRepository.deleteAll();
	}

	@Test
	void syncSchedulesFetchesCurrentAndNextMonthsAndUpsertsByKey() {
		when(client.fetchSchedules(any())).thenReturn(List.of(schedule("1-OPI001", 7, "정기 평가")));

		syncService.syncSchedules();
		when(client.fetchSchedules(any())).thenReturn(List.of(schedule("1-OPI001", 7, "정기 평가 (변경)")));
		OpicSyncWriter.ScheduleSyncResult result = syncService.syncSchedules();

		verify(client, times(6)).fetchSchedules(any()); // months-ahead 2 → 3 months per run
		assertThat(result.upserted()).isEqualTo(1);
		assertThat(result.removed()).isZero();
		assertThat(scheduleRepository.count()).isEqualTo(1);
		assertThat(queryService.schedules(NEXT_MONTH))
			.singleElement()
			.satisfies(s -> assertThat(s.getRoundName()).isEqualTo("정기 평가 (변경)"));
		assertThat(queryService.schedules(NEXT_MONTH.plusMonths(1))).isEmpty();
	}

	@Test
	void syncSchedulesRemovesRoundsThatDisappearedWithinFetchedMonths() {
		when(client.fetchSchedules(any())).thenReturn(List.of(
			schedule("1-OPI001", 7, "정기 평가"),
			schedule("2-OPI001", 14, "정기 평가")
		));
		syncService.syncSchedules();

		when(client.fetchSchedules(any())).thenReturn(List.of(schedule("1-OPI001", 7, "정기 평가")));
		OpicSyncWriter.ScheduleSyncResult result = syncService.syncSchedules();

		assertThat(result.removed()).isEqualTo(1);
		assertThat(queryService.schedules(NEXT_MONTH)).extracting(s -> s.getExternalKey()).containsExactly("1-OPI001");
	}

	@Test
	void emptyScheduleFetchRemovesNothing() {
		when(client.fetchSchedules(any())).thenReturn(List.of(schedule("1-OPI001", 7, "정기 평가")));
		syncService.syncSchedules();

		when(client.fetchSchedules(any())).thenReturn(List.of());
		OpicSyncWriter.ScheduleSyncResult result = syncService.syncSchedules();

		assertThat(result.removed()).isZero();
		assertThat(scheduleRepository.count()).isEqualTo(1);
	}

	@Test
	void syncCentersDeactivatesCentersMissingFromLatestList() {
		when(client.fetchCenters()).thenReturn(List.of(center("A", "서울"), center("B", "경기")));
		syncService.syncCenters();

		when(client.fetchCenters()).thenReturn(List.of(center("A", "서울")));
		OpicSyncWriter.CenterSyncResult result = syncService.syncCenters();

		assertThat(result.upserted()).isEqualTo(1);
		assertThat(result.deactivated()).isEqualTo(1);
		assertThat(queryService.centers(null)).extracting(OpicTestCenter::getName).containsExactly("센터 A");
		assertThat(queryService.centers("경기")).isEmpty();
		assertThat(centerRepository.count()).isEqualTo(2);
	}

	@Test
	void emptyCenterListChangesNothing() {
		when(client.fetchCenters()).thenReturn(List.of(center("A", "서울")));
		syncService.syncCenters();

		when(client.fetchCenters()).thenReturn(List.of());
		OpicSyncWriter.CenterSyncResult result = syncService.syncCenters();

		assertThat(result.deactivated()).isZero();
		assertThat(queryService.centers("서울")).hasSize(1);
	}

	@Test
	void fetchFailureBecomesOpicSyncFailed() {
		when(client.fetchCenters()).thenThrow(new IllegalStateException("timeout"));

		assertThatThrownBy(() -> syncService.syncCenters())
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OPIC_SYNC_FAILED));
	}

	/** A round on {@code day} of next month, which is always inside the fetched window. */
	private static ScheduleRow schedule(String key, int day, String name) {
		LocalDate exam = NEXT_MONTH.atDay(day);
		return new ScheduleRow(key, exam, exam.minusDays(30), exam.minusDays(7), exam.plusDays(5), name, false);
	}

	private static CenterRow center(String id, String region) {
		return new CenterRow("센터 " + id + "|주소 " + id, "센터 " + id, "주소 " + id, 37.5, 127.0,
			null, "1호선", "역에서 도보 5분", region.equals("서울") ? "A" : "C001", region);
	}
}
