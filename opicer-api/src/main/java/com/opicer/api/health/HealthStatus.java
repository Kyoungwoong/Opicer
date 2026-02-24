package com.opicer.api.health;

import java.time.Instant;

public record HealthStatus(String status, Instant timestamp) {}
