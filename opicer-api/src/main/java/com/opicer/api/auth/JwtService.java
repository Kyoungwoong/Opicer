package com.opicer.api.auth;

import com.opicer.api.config.AuthProperties;
import com.opicer.api.user.AuthProvider;
import com.opicer.api.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final AuthProperties authProperties;
	private final SecretKey key;

	public JwtService(AuthProperties authProperties) {
		this.authProperties = authProperties;
		this.key = Keys.hmacShaKeyFor(authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
	}

	public String issueToken(AuthUserPrincipal principal) {
		Instant now = Instant.now();
		Instant expiry = now.plusSeconds(authProperties.getJwtTtlSeconds());
		return Jwts.builder()
			.subject(principal.id().toString())
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiry))
			.claim("email", principal.email())
			.claim("name", principal.name())
			.claim("role", principal.role().name())
			.claim("provider", principal.provider().name())
			.signWith(key)
			.compact();
	}

	public AuthUserPrincipal parseToken(String token) {
		Claims claims = Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload();

		UUID id = UUID.fromString(claims.getSubject());
		String email = claims.get("email", String.class);
		String name = claims.get("name", String.class);
		UserRole role = UserRole.valueOf(claims.get("role", String.class));
		AuthProvider provider = AuthProvider.valueOf(claims.get("provider", String.class));
		return new AuthUserPrincipal(id, email, name, role, provider);
	}
}
