package com.opicer.api.aijob.application;

import com.opicer.api.config.WorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls for due jobs. Registered only with {@code opicer.ai.worker.enabled=true}. */
@Component
@ConditionalOnProperty(name = WorkerProperties.ENABLED_KEY, havingValue = "true")
public class AiJobScheduler {

	private static final Logger log = LoggerFactory.getLogger(AiJobScheduler.class);

	private final AiJobRunner runner;

	public AiJobScheduler(AiJobRunner runner) {
		this.runner = runner;
		log.info("[AiJob] worker enabled");
	}

	@Scheduled(fixedDelayString = "${opicer.ai.worker.poll-interval-ms:2000}")
	public void poll() {
		try {
			runner.runOnce();
		} catch (RuntimeException e) {
			// Keep polling; a database hiccup must not stop the schedule.
			log.error("[AiJob] poll failed: {}", e.getMessage());
		}
	}
}
