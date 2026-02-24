package com.opicer.api.auth.application;

import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

	@Test
	void issuesAndParsesToken() {
		AuthProperties props = new AuthProperties();
		props.setJwtSecret("change-me-please-change-me-please-32bytes");
		props.setJwtTtlSeconds(3600);

		JwtService jwtService = new JwtService(props);
		AuthUserPrincipal principal = new AuthUserPrincipal(
			UUID.randomUUID(),
			"test@example.com",
			"Tester",
			UserRole.USER,
			AuthProvider.KAKAO
		);

		String token = jwtService.issueToken(principal);
		AuthUserPrincipal parsed = jwtService.parseToken(token);

		assertThat(parsed.email()).isEqualTo(principal.email());
		assertThat(parsed.name()).isEqualTo(principal.name());
		assertThat(parsed.role()).isEqualTo(principal.role());
		assertThat(parsed.provider()).isEqualTo(principal.provider());
	}
}
