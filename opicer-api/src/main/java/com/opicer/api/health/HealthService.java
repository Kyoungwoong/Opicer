package com.opicer.api.health;

import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

	public HealthStatus getStatus() {
		return new HealthStatus("ok", Instant.now());
	}
}
