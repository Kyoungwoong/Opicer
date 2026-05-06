package com.opicer.api.practice.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.practice.application.PracticeSessionCommandService;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PracticeSessionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@MockBean
	private PracticeSessionCommandService practiceSessionCommandService;

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

	@Test
	void submitSuccessReturns200() throws Exception {
		PracticeSessionCommandService.SubmitResult result = new PracticeSessionCommandService.SubmitResult(
			UUID.randomUUID(),
			false,
			2
		);
		when(practiceSessionCommandService.submit(any(), any())).thenReturn(result);

		mockMvc.perform(post("/api/practice/sessions/submit")
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "topicSelectionId": "11111111-1111-1111-1111-111111111111"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.deductedCredits").value(2));
	}

	@Test
	void submitInsufficientBalanceReturns402() throws Exception {
		when(practiceSessionCommandService.submit(any(), any()))
			.thenThrow(new ApiException(ErrorCode.CREDIT_INSUFFICIENT_BALANCE));

		mockMvc.perform(post("/api/practice/sessions/submit")
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "topicSelectionId": "11111111-1111-1111-1111-111111111111"
					}
					"""))
			.andExpect(status().isPaymentRequired())
			.andExpect(jsonPath("$.code").value("CREDIT_INSUFFICIENT_BALANCE"));
	}
}
