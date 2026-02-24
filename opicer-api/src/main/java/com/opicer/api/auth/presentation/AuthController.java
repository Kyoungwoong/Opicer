package com.opicer.api.auth.presentation;

import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthProperties authProperties;

	public AuthController(AuthProperties authProperties) {
		this.authProperties = authProperties;
	}

	@GetMapping("/me")
	public ResponseEntity<AuthMeResponse> me(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
			return ResponseEntity.status(401).build();
		}
		AuthMeResponse response = new AuthMeResponse(
			principal.id(),
			principal.email(),
			principal.name(),
			principal.role().name(),
			principal.provider().name()
		);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from(authProperties.getCookieName(), "")
			.httpOnly(true)
			.secure(authProperties.isCookieSecure())
			.sameSite("Lax")
			.path("/")
			.maxAge(0)
			.build();
		response.addHeader("Set-Cookie", cookie.toString());
		return ResponseEntity.noContent().build();
	}

	public record AuthMeResponse(
		UUID id,
		String email,
		String name,
		String role,
		String provider
	) {}
}
