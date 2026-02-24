package com.opicer.api.auth;

import com.opicer.api.config.AuthProperties;
import com.opicer.api.user.AuthProvider;
import com.opicer.api.user.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
	"opicer.auth.jwt-secret=change-me-please-change-me-please-32bytes",
	"opicer.auth.jwt-ttl-seconds=3600"
})
@AutoConfigureMockMvc
@Import(AuthControllerTest.AdminTestConfig.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@Test
	void meWithoutJwtReturns401() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void meWithJwtReturns200() throws Exception {
		String token = jwtService.issueToken(new AuthUserPrincipal(
			UUID.randomUUID(),
			"user@example.com",
			"User",
			UserRole.USER,
			AuthProvider.KAKAO
		));

		mockMvc.perform(get("/api/auth/me")
				.cookie(new jakarta.servlet.http.Cookie(authProperties.getCookieName(), token)))
			.andExpect(status().isOk());
	}

	@Test
	void adminEndpointRequiresAdminRole() throws Exception {
		String token = jwtService.issueToken(new AuthUserPrincipal(
			UUID.randomUUID(),
			"user@example.com",
			"User",
			UserRole.USER,
			AuthProvider.KAKAO
		));

		mockMvc.perform(get("/api/admin/ping")
				.cookie(new jakarta.servlet.http.Cookie(authProperties.getCookieName(), token))
				.contentType(MediaType.TEXT_PLAIN))
			.andExpect(status().isForbidden());
	}

	@TestConfiguration
	static class AdminTestConfig {
		@Bean
		AdminTestController adminTestController() {
			return new AdminTestController();
		}
	}

	@RestController
	@RequestMapping("/api/admin")
	static class AdminTestController {
		@GetMapping("/ping")
		String ping() {
			return "ok";
		}
	}
}
