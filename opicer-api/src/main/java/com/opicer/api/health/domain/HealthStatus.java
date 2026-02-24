package com.opicer.api.health.domain;

import java.time.Instant;

public record HealthStatus(String status, Instant timestamp) {}
