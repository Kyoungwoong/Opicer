package com.opicer.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opicer.credit")
public class CreditProperties {

	private long unsafeDelayMs = 0;
	private long idempotencyTtlHours = 24;

	public long getUnsafeDelayMs() {
		return unsafeDelayMs;
	}

	public void setUnsafeDelayMs(long unsafeDelayMs) {
		this.unsafeDelayMs = unsafeDelayMs;
	}

	public long getIdempotencyTtlHours() {
		return idempotencyTtlHours;
	}

	public void setIdempotencyTtlHours(long idempotencyTtlHours) {
		this.idempotencyTtlHours = idempotencyTtlHours;
	}
}
