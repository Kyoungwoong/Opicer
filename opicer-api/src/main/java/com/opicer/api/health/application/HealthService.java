package com.opicer.api.health.application;

import com.opicer.api.health.domain.HealthStatus;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

	public HealthStatus getStatus() {
		return new HealthStatus("ok", Instant.now());
	}
}
