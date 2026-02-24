package com.opicer.api.auth.domain;

import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import java.util.UUID;

public record AuthUserPrincipal(
	UUID id,
	String email,
	String name,
	UserRole role,
	AuthProvider provider
) {}
