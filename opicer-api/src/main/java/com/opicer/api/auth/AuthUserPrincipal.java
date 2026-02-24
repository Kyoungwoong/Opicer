package com.opicer.api.auth;

import com.opicer.api.user.AuthProvider;
import com.opicer.api.user.UserRole;
import java.util.UUID;

public record AuthUserPrincipal(
	UUID id,
	String email,
	String name,
	UserRole role,
	AuthProvider provider
) {}
