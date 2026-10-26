package com.opicer.api.opic.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily schedule sync and weekly center sync. Only registered when opicer.opic-sync.enabled=true. */
@Component
@ConditionalOnProperty(prefix = "opicer.opic-sync", name = "enabled", havingValue = "true")
public class OpicSyncScheduler {

	private static final Logger log = LoggerFactory.getLogger(OpicSyncScheduler.class);

	private final OpicSyncService syncService;

	public OpicSyncScheduler(OpicSyncService syncService) {
		this.syncService = syncService;
	}

	@Scheduled(cron = "${opicer.opic-sync.schedule-cron:0 30 4 * * *}", zone = "Asia/Seoul")
	public void syncSchedules() {
		try {
			syncService.syncSchedules();
		} catch (RuntimeException e) {
			log.warn("[OpicSync] scheduled schedule sync failed: {}", e.getMessage());
		}
	}

	@Scheduled(cron = "${opicer.opic-sync.center-cron:0 0 5 * * MON}", zone = "Asia/Seoul")
	public void syncCenters() {
		try {
			syncService.syncCenters();
		} catch (RuntimeException e) {
			log.warn("[OpicSync] scheduled center sync failed: {}", e.getMessage());
		}
	}
}
