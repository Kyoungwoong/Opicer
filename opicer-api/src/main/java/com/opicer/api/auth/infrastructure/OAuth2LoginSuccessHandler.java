package com.opicer.api.auth.infrastructure;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.user.application.UserService;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.User;
import com.opicer.api.user.domain.UserRole;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final UserService userService;
	private final JwtService jwtService;
	private final AuthProperties authProperties;

	public OAuth2LoginSuccessHandler(UserService userService, JwtService jwtService, AuthProperties authProperties) {
		this.userService = userService;
		this.jwtService = jwtService;
		this.authProperties = authProperties;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
		throws IOException, ServletException {

		OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
		Map<String, Object> attributes = oauth2User.getAttributes();

		KakaoProfile kakao = KakaoProfile.from(attributes);
		UserRole role = resolveRole(kakao.email());
		User user = userService.upsert(AuthProvider.KAKAO, kakao.id(), kakao.email(), kakao.nickname(), role);

		AuthUserPrincipal principal = new AuthUserPrincipal(
			user.getId(),
			user.getEmail(),
			user.getName(),
			user.getRole(),
			user.getProvider()
		);

		String token = jwtService.issueToken(principal);
		ResponseCookie cookie = ResponseCookie.from(authProperties.getCookieName(), token)
			.httpOnly(true)
			.secure(authProperties.isCookieSecure())
			.sameSite("Lax")
			.path("/")
			.maxAge(authProperties.getJwtTtlSeconds())
			.build();
		response.addHeader("Set-Cookie", cookie.toString());
		response.sendRedirect(authProperties.getFrontendBaseUrl() + "/auth/success");
	}

	private UserRole resolveRole(String email) {
		if (email == null) {
			return UserRole.USER;
		}
		Set<String> allowlist = authProperties.getAdminAllowlist().stream()
			.map(String::trim)
			.map(value -> value.toLowerCase(Locale.ROOT))
			.filter(value -> !value.isEmpty())
			.collect(Collectors.toSet());
		if (allowlist.contains(email.toLowerCase(Locale.ROOT))) {
			return UserRole.ADMIN;
		}
		return UserRole.USER;
	}

	private record KakaoProfile(String id, String email, String nickname) {
		static KakaoProfile from(Map<String, Object> attributes) {
			String id = String.valueOf(attributes.get("id"));
			Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
			String email = null;
			String nickname = null;
			if (account != null) {
				email = (String) account.get("email");
				Map<String, Object> profile = (Map<String, Object>) account.get("profile");
				if (profile != null) {
					nickname = (String) profile.get("nickname");
				}
			}
			return new KakaoProfile(id, email, nickname);
		}
	}
}
