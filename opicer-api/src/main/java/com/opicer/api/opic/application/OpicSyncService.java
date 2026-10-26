package com.opicer.api.opic.application;

import com.opicer.api.config.OpicSyncProperties;
import com.opicer.api.opic.infrastructure.OpicSiteClient;
import com.opicer.api.opic.infrastructure.OpicSiteParser.ScheduleRow;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Pulls schedules/centers from opic.or.kr and stores them. Requests are paced by {@code requestDelayMs}. */
@Service
public class OpicSyncService {

	private static final Logger log = LoggerFactory.getLogger(OpicSyncService.class);
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final OpicSiteClient client;
	private final OpicSyncWriter writer;
	private final OpicSyncProperties properties;

	public OpicSyncService(OpicSiteClient client, OpicSyncWriter writer, OpicSyncProperties properties) {
		this.client = client;
		this.writer = writer;
		this.properties = properties;
	}

	/** Current month plus {@code monthsAhead} months. */
	public int syncSchedules() {
		YearMonth first = YearMonth.now(SEOUL);
		List<ScheduleRow> rows = new ArrayList<>();
		for (int i = 0; i <= properties.getMonthsAhead(); i++) {
			if (i > 0) {
				pause();
			}
			YearMonth month = first.plusMonths(i);
			rows.addAll(fetch(() -> client.fetchSchedules(month), "schedules " + month));
		}
		int written = writer.upsertSchedules(rows, Instant.now());
		log.info("[OpicSync] schedules upserted={}", written);
		return written;
	}

	public OpicSyncWriter.CenterSyncResult syncCenters() {
		OpicSyncWriter.CenterSyncResult result = writer.upsertCenters(fetch(client::fetchCenters, "centers"), Instant.now());
		log.info("[OpicSync] centers upserted={} deactivated={}", result.upserted(), result.deactivated());
		return result;
	}

	private <T> T fetch(Supplier<T> call, String what) {
		try {
			return call.get();
		} catch (RuntimeException e) {
			log.warn("[OpicSync] fetching {} failed: {}", what, e.getMessage());
			throw new ApiException(ErrorCode.OPIC_SYNC_FAILED, "Failed to fetch " + what + " from opic.or.kr");
		}
	}

	private void pause() {
		if (properties.getRequestDelayMs() <= 0) {
			return;
		}
		try {
			Thread.sleep(properties.getRequestDelayMs());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
