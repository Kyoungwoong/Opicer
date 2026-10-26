package com.opicer.api.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Background AI job worker. Disabled unless {@code enabled} is set (on locally, off in tests and production). */
@ConfigurationProperties(prefix = "opicer.ai.worker")
public class WorkerProperties {

	public static final String ENABLED_KEY = "opicer.ai.worker.enabled";

	private boolean enabled = false;
	private long pollIntervalMs = 2000;
	/** Jobs claimed per poll. */
	private int batchSize = 2;
	/** Answers evaluated in parallel within one session. */
	private int concurrency = 3;
	/** Automatic attempts per job before it is finalised as failed/partial. */
	private int maxAttempts = 3;
	/** Delay before attempt n+1, indexed by failed attempt count (last value repeats). */
	private List<Integer> backoffSeconds = List.of(15, 60, 300);
	/** A RUNNING job locked longer than this is assumed abandoned and released. */
	private int staleAfterMinutes = 10;
	/** Free manual retries a learner may request per session. */
	private int maxManualRetries = 2;

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public long getPollIntervalMs() { return pollIntervalMs; }
	public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
	public int getBatchSize() { return batchSize; }
	public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
	public int getConcurrency() { return concurrency; }
	public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
	public int getMaxAttempts() { return maxAttempts; }
	public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
	public List<Integer> getBackoffSeconds() { return backoffSeconds; }
	public void setBackoffSeconds(List<Integer> backoffSeconds) { this.backoffSeconds = backoffSeconds; }
	public int getStaleAfterMinutes() { return staleAfterMinutes; }
	public void setStaleAfterMinutes(int staleAfterMinutes) { this.staleAfterMinutes = staleAfterMinutes; }
	public int getMaxManualRetries() { return maxManualRetries; }
	public void setMaxManualRetries(int maxManualRetries) { this.maxManualRetries = maxManualRetries; }
}
