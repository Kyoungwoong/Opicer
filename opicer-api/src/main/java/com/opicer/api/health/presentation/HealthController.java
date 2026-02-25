package com.opicer.api.health.presentation;

import com.opicer.api.health.application.HealthService;
import com.opicer.api.health.domain.HealthStatus;
import com.opicer.api.shared.presentation.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

	private final HealthService healthService;

	public HealthController(HealthService healthService) {
		this.healthService = healthService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<HealthStatus>> getHealth() {
		HealthStatus status = healthService.getStatus();
		return ResponseEntity.ok(ApiResponse.ok("HEALTH_OK", status));
	}
}
