package com.opicer.api.practice.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.practice.application.PracticeAiService;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
class PracticeAiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@MockBean
	private PracticeAiService practiceAiService;

	private Cookie userCookie;

	@BeforeEach
	void setUp() {
		userCookie = new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(
				UUID.randomUUID(),
				"user@example.com",
				"User",
				UserRole.USER,
				AuthProvider.KAKAO
			)
		));
	}

	// T1: POST /transcribe valid multipart + auth → 200
	@Test
	void transcribe_validRequest_returns200() throws Exception {
		when(practiceAiService.transcribe(any(), anyString())).thenReturn("Hello world");

		MockMultipartFile audioFile = new MockMultipartFile(
			"audio", "audio.webm", "audio/webm", "fake-audio".getBytes());

		mockMvc.perform(multipart("/api/practice/transcribe")
				.file(audioFile)
				.param("questionText", "Tell me about yourself.")
				.cookie(userCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.transcript").value("Hello world"));
	}

	// T2: POST /transcribe 인증 없음 → 401
	@Test
	void transcribe_noAuth_returns401() throws Exception {
		MockMultipartFile audioFile = new MockMultipartFile(
			"audio", "audio.webm", "audio/webm", "fake-audio".getBytes());

		mockMvc.perform(multipart("/api/practice/transcribe")
				.file(audioFile)
				.param("questionText", "Tell me about yourself."))
			.andExpect(status().isUnauthorized());
	}

	// T3: POST /transcribe AI_NOT_CONFIGURED → 503
	@Test
	void transcribe_aiNotConfigured_returns503() throws Exception {
		when(practiceAiService.transcribe(any(), anyString()))
			.thenThrow(new ApiException(ErrorCode.AI_NOT_CONFIGURED));

		MockMultipartFile audioFile = new MockMultipartFile(
			"audio", "audio.webm", "audio/webm", "fake-audio".getBytes());

		mockMvc.perform(multipart("/api/practice/transcribe")
				.file(audioFile)
				.param("questionText", "Tell me about yourself.")
				.cookie(userCookie))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.code").value("AI_NOT_CONFIGURED"));
	}

	// T4: POST /analyze valid JSON + auth → 200
	@Test
	void analyze_validRequest_returns200() throws Exception {
		when(practiceAiService.analyze(anyString(), anyString())).thenReturn("Analysis result");

		mockMvc.perform(post("/api/practice/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"questionText\":\"Tell me about yourself.\",\"transcript\":\"I am a student.\"}")
				.cookie(userCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.analysis").value("Analysis result"));
	}

	// T5: POST /analyze 인증 없음 → 401
	@Test
	void analyze_noAuth_returns401() throws Exception {
		mockMvc.perform(post("/api/practice/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"questionText\":\"Tell me about yourself.\",\"transcript\":\"I am a student.\"}"))
			.andExpect(status().isUnauthorized());
	}

	// T6: POST /analyze blank fields → 400
	@Test
	void analyze_blankFields_returns400() throws Exception {
		mockMvc.perform(post("/api/practice/analyze")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"questionText\":\"\",\"transcript\":\"\"}")
				.cookie(userCookie))
			.andExpect(status().isBadRequest());
	}

	// T7: POST /improve valid JSON + auth → 200
	@Test
	void improve_validRequest_returns200() throws Exception {
		when(practiceAiService.improve(anyString(), anyString())).thenReturn("Improved script");

		mockMvc.perform(post("/api/practice/improve")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"questionText\":\"Tell me about yourself.\",\"transcript\":\"I am a student.\"}")
				.cookie(userCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.improved").value("Improved script"));
	}

	// T8: POST /improve 인증 없음 → 401
	@Test
	void improve_noAuth_returns401() throws Exception {
		mockMvc.perform(post("/api/practice/improve")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"questionText\":\"Tell me about yourself.\",\"transcript\":\"I am a student.\"}"))
			.andExpect(status().isUnauthorized());
	}
}
