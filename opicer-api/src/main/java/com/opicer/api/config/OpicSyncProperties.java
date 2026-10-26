package com.opicer.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sync of public OPIc exam schedules / test centers from opic.or.kr.
 * opic.or.kr's robots.txt disallows generic crawlers, so the scheduled job is off unless enabled,
 * and requests are kept to a few per day.
 */
@ConfigurationProperties(prefix = "opicer.opic-sync")
public class OpicSyncProperties {

	private boolean enabled = false;
	private String baseUrl = "https://www.opic.or.kr/opics/servlet";
	private String userAgent = "OpicerBot/0.1 (+https://github.com/Kyoungwoong/Opicer)";
	private int monthsAhead = 2;
	private long requestDelayMs = 1500;
	private int timeoutMs = 10000;

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }

	public String getBaseUrl() { return baseUrl; }
	public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

	public String getUserAgent() { return userAgent; }
	public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

	public int getMonthsAhead() { return monthsAhead; }
	public void setMonthsAhead(int monthsAhead) { this.monthsAhead = monthsAhead; }

	public long getRequestDelayMs() { return requestDelayMs; }
	public void setRequestDelayMs(long requestDelayMs) { this.requestDelayMs = requestDelayMs; }

	public int getTimeoutMs() { return timeoutMs; }
	public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}
