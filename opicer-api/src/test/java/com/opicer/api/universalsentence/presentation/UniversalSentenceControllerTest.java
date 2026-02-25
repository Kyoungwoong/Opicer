package com.opicer.api.universalsentence.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.universalsentence.domain.UniversalSentence;
import com.opicer.api.universalsentence.domain.UniversalSentenceType;
import com.opicer.api.universalsentence.infrastructure.UniversalSentenceRepository;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
	"opicer.auth.jwt-secret=change-me-please-change-me-please-32bytes",
	"opicer.auth.jwt-ttl-seconds=3600"
})
@AutoConfigureMockMvc
class UniversalSentenceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@Autowired
	private UniversalSentenceRepository universalSentenceRepository;

	private jakarta.servlet.http.Cookie userCookie;
	private jakarta.servlet.http.Cookie adminCookie;

	@BeforeEach
	void setUp() {
		universalSentenceRepository.deleteAll();
		userCookie = new jakarta.servlet.http.Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(
				UUID.randomUUID(),
				"user@example.com",
				"User",
				UserRole.USER,
				AuthProvider.KAKAO
			)
		));
		adminCookie = new jakarta.servlet.http.Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(
				UUID.randomUUID(),
				"admin@example.com",
				"Admin",
				UserRole.ADMIN,
				AuthProvider.KAKAO
			)
		));
	}

	@Test
	void randomEndpointReturnsFourActiveSentences() throws Exception {
		List<UniversalSentence> seeds = List.of(
			new UniversalSentence(UniversalSentenceType.OPINION, "Opinion start",
				"From my perspective, ...", List.of("opinion", "starter"), true),
			new UniversalSentence(UniversalSentenceType.OPINION, "Opinion support",
				"I believe this because ...", List.of("opinion"), true),
			new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "Past experience",
				"I still remember the time ...", List.of("experience"), true),
			new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "Past detail",
				"Back then, I had to ...", List.of("past"), true),
			new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "Compare",
			"Compared to the past ...", List.of("compare"), true),
			new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "Problem solving",
				"In that situation ...", List.of("solution"), true),
			new UniversalSentence(UniversalSentenceType.OPINION, "Inactive",
				"Should not appear", List.of("inactive"), false)
		);
		universalSentenceRepository.saveAll(seeds);

		mockMvc.perform(get("/api/universal-sentences/random?size=4").cookie(userCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(4));
	}

	@Test
	void dailyEndpointReturnsFourSentences() throws Exception {
		List<UniversalSentence> seeds = List.of(
			new UniversalSentence(UniversalSentenceType.OPINION, "Opinion", "From my perspective ...",
				List.of("opinion"), true),
			new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "Past", "I remember ...",
				List.of("past"), true),
			new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "Compare", "Compared to ...",
				List.of("compare"), true),
			new UniversalSentence(UniversalSentenceType.UNEXPECTED_SITUATION, "Unexpected", "In that situation ...",
				List.of("unexpected"), true)
		);
		universalSentenceRepository.saveAll(seeds);

		mockMvc.perform(get("/api/universal-sentences/daily").cookie(userCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(4));
	}

	@Test
	void dailyEndpointMissingTypeReturnsErrorCode() throws Exception {
		List<UniversalSentence> seeds = List.of(
			new UniversalSentence(UniversalSentenceType.OPINION, "Opinion", "From my perspective ...",
				List.of("opinion"), true),
			new UniversalSentence(UniversalSentenceType.PAST_EXPERIENCE, "Past", "I remember ...",
				List.of("past"), true),
			new UniversalSentence(UniversalSentenceType.COMPARE_CONTRAST, "Compare", "Compared to ...",
				List.of("compare"), true)
		);
		universalSentenceRepository.saveAll(seeds);

		mockMvc.perform(get("/api/universal-sentences/daily").cookie(userCookie))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("UNIVERSAL_SENTENCE_DAILY_NOT_READY"));
	}

	@Test
	void adminUpdateNotFoundReturnsErrorCode() throws Exception {
		mockMvc.perform(put("/api/admin/universal-sentences/{id}", UUID.randomUUID())
				.cookie(adminCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "type": "OPINION",
					  "title": "Opinion",
					  "sentence": "From my perspective ...",
					  "tags": ["opinion"],
					  "active": true
					}
					"""))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("UNIVERSAL_SENTENCE_NOT_FOUND"));
	}
}
