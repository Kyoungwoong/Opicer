package com.opicer.api.shared.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
	"opicer.auth.jwt-secret=change-me-please-change-me-please-32bytes",
	"opicer.auth.jwt-ttl-seconds=3600"
})
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerTest.TestApiConfig.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	private jakarta.servlet.http.Cookie authCookie;

	@BeforeEach
	void setUp() {
		String token = jwtService.issueToken(new AuthUserPrincipal(
			UUID.randomUUID(),
			"user@example.com",
			"User",
			UserRole.USER,
			AuthProvider.KAKAO
		));
		authCookie = new jakarta.servlet.http.Cookie(authProperties.getCookieName(), token);
	}

	@Test
	void validationErrorReturnsFieldErrors() throws Exception {
		mockMvc.perform(post("/api/test/validation")
				.cookie(authCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors[0].field").value("name"));
	}

	@Test
	void apiExceptionNotFoundReturnsCode() throws Exception {
		mockMvc.perform(get("/api/test/not-found").cookie(authCookie))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
	}

	@Test
	void apiExceptionConflictReturnsCode() throws Exception {
		mockMvc.perform(get("/api/test/duplicate").cookie(authCookie))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_DATE"));
	}

	@Test
	void dataIntegrityViolationReturnsCode() throws Exception {
		mockMvc.perform(get("/api/test/db").cookie(authCookie))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"));
	}

	@Test
	void typeMismatchReturnsBadRequest() throws Exception {
		mockMvc.perform(get("/api/test/type").param("count", "abc").cookie(authCookie))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@Test
	void malformedJsonReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/test/validation")
				.cookie(authCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{bad-json"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	@TestConfiguration
	static class TestApiConfig {
		@Bean
		TestApiController testApiController() {
			return new TestApiController();
		}
	}

	@RestController
	@RequestMapping("/api/test")
	static class TestApiController {

		@PostMapping("/validation")
		String validation(@Valid @RequestBody TestRequest request) {
			return "ok";
		}

		@GetMapping("/not-found")
		String notFound() {
			throw new ApiException(ErrorCode.QUESTION_NOT_FOUND);
		}

		@GetMapping("/duplicate")
		String duplicate() {
			throw new ApiException(ErrorCode.DUPLICATE_DATE);
		}

		@GetMapping("/db")
		String dbError() {
			throw new DataIntegrityViolationException("constraint");
		}

		@GetMapping("/type")
		String type(@RequestParam int count) {
			return "ok";
		}
	}

	public record TestRequest(@NotBlank String name) {}
}
