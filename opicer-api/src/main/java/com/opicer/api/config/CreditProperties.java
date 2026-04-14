package com.opicer.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opicer.credit")
public class CreditProperties {

	private long unsafeDelayMs = 0;

	public long getUnsafeDelayMs() {
		return unsafeDelayMs;
	}

	public void setUnsafeDelayMs(long unsafeDelayMs) {
		this.unsafeDelayMs = unsafeDelayMs;
	}
}
