package com.opicer.api.practice.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.ai.application.AiTranscriptionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
class PracticeTranscriptionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@MockBean
	private AiTranscriptionService transcriptionService;

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
		when(transcriptionService.transcribe(any(), anyString())).thenReturn("Hello world");

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
		when(transcriptionService.transcribe(any(), anyString()))
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

	// The manual analyze/improve endpoints were replaced by automatic evaluation and are gone.
	@Test
	void wrongMethodOnAnExistingPathIs405() throws Exception {
		mockMvc.perform(get("/api/practice/transcribe")
				.cookie(userCookie))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void removedAnalyzeAndImproveEndpointsAreNotServed() throws Exception {
		for (String path : new String[] {"/api/practice/analyze", "/api/practice/improve"}) {
			mockMvc.perform(post(path)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"topicId\":\"11111111-1111-1111-1111-111111111111\",\"questionText\":\"Q\",\"transcript\":\"A\"}")
					.cookie(userCookie))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
		}
	}
}
