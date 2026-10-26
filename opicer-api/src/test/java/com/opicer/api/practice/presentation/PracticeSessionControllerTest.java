package com.opicer.api.practice.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.practice.application.PracticeHistoryCommandService;
import com.opicer.api.practice.application.PracticeHistoryQueryService;
import com.opicer.api.practice.application.PracticeSessionCommandService;
import com.opicer.api.practice.domain.SessionEvaluationStatus;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import jakarta.servlet.http.Cookie;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PracticeSessionControllerTest {

	private static final String SESSION_ID = "22222222-2222-2222-2222-222222222222";
	private static final String QUESTION_ID = "33333333-3333-3333-3333-333333333333";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@MockBean
	private PracticeSessionCommandService practiceSessionCommandService;

	@MockBean
	private PracticeHistoryQueryService practiceHistoryQueryService;

	@MockBean
	private PracticeHistoryCommandService practiceHistoryCommandService;

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
		UUID sessionId = UUID.randomUUID();
		PracticeSessionCommandService.SubmitResult result = new PracticeSessionCommandService.SubmitResult(
			UUID.randomUUID(),
			false,
			2,
			sessionId,
			SessionEvaluationStatus.PENDING
		);
		when(practiceSessionCommandService.submit(any(), any(), any(), any())).thenReturn(result);

		mockMvc.perform(post("/api/practice/sessions/submit")
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "topicSelectionId": "11111111-1111-1111-1111-111111111111"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.deductedCredits").value(2))
			.andExpect(jsonPath("$.data.sessionId").value(sessionId.toString()))
			.andExpect(jsonPath("$.data.evaluationStatus").value("PENDING"));
	}

	@Test
	void submitWithAnswersPassesThemToService() throws Exception {
		when(practiceSessionCommandService.submit(any(), any(), any(), any())).thenReturn(
			new PracticeSessionCommandService.SubmitResult(UUID.randomUUID(), false, 2, UUID.randomUUID(), null)
		);

		mockMvc.perform(post("/api/practice/sessions/submit")
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "topicSelectionId": "11111111-1111-1111-1111-111111111111",
					  "answers": [
					    {"questionId": "%s", "questionText": "Describe your home.", "transcript": "I live in an apartment."}
					  ]
					}
					""".formatted(QUESTION_ID)))
			.andExpect(status().isOk());

		verify(practiceSessionCommandService).submit(any(), any(), isNull(), eq(List.of(
			new PracticeSessionCommandService.AnswerInput(
				UUID.fromString(QUESTION_ID), "Describe your home.", "I live in an apartment."
			)
		)));
	}

	@Test
	void submitPassesTargetLevelAndRecordingLengths() throws Exception {
		when(practiceSessionCommandService.submit(any(), any(), any(), any())).thenReturn(
			new PracticeSessionCommandService.SubmitResult(UUID.randomUUID(), false, 2, UUID.randomUUID(),
				SessionEvaluationStatus.PENDING)
		);

		mockMvc.perform(post("/api/practice/sessions/submit")
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "topicSelectionId": "11111111-1111-1111-1111-111111111111",
					  "targetLevel": "IH",
					  "answers": [
					    {"questionId": "%s", "questionText": "Describe your home.", "transcript": "I live here.", "durationSeconds": 74}
					  ]
					}
					""".formatted(QUESTION_ID)))
			.andExpect(status().isOk());

		verify(practiceSessionCommandService).submit(any(), any(), eq(OpicLevel.IH), eq(List.of(
			new PracticeSessionCommandService.AnswerInput(UUID.fromString(QUESTION_ID), "Describe your home.", "I live here.", 74)
		)));
	}

	@Test
	void submitRejectsOutOfRangeRecordingLength() throws Exception {
		mockMvc.perform(post("/api/practice/sessions/submit")
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "topicSelectionId": "11111111-1111-1111-1111-111111111111",
					  "answers": [{"questionId": "%s", "questionText": "Q", "transcript": "a", "durationSeconds": 0}]
					}
					""".formatted(QUESTION_ID)))
			.andExpect(status().isBadRequest());

		verify(practiceSessionCommandService, never()).submit(any(), any(), any(), any());
	}

	@Test
	void submitRejectsUnknownTargetLevel() throws Exception {
		mockMvc.perform(post("/api/practice/sessions/submit")
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"topicSelectionId": "11111111-1111-1111-1111-111111111111", "targetLevel": "IM2"}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void submitWithDuplicateQuestionIdsReturns400() throws Exception {
		mockMvc.perform(post("/api/practice/sessions/submit")
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "topicSelectionId": "11111111-1111-1111-1111-111111111111",
					  "answers": [
					    {"questionId": "%1$s", "questionText": "Q1", "transcript": "a"},
					    {"questionId": "%1$s", "questionText": "Q1", "transcript": "b"}
					  ]
					}
					""".formatted(QUESTION_ID)))
			.andExpect(status().isBadRequest());

		verify(practiceSessionCommandService, never()).submit(any(), any(), any(), any());
	}

	@Test
	void submitInsufficientBalanceReturns402() throws Exception {
		when(practiceSessionCommandService.submit(any(), any(), any(), any()))
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

	@Test
	void listWithoutAuthReturns401() throws Exception {
		mockMvc.perform(get("/api/practice/sessions"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void listReturns200() throws Exception {
		when(practiceHistoryQueryService.list(any(), anyInt())).thenReturn(List.of());

		mockMvc.perform(get("/api/practice/sessions").param("limit", "3").cookie(userCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray());

		verify(practiceHistoryQueryService).list(any(), eq(3));
	}

	@Test
	void getMissingSessionReturns404() throws Exception {
		when(practiceHistoryQueryService.get(any(), any()))
			.thenThrow(new ApiException(ErrorCode.PRACTICE_SESSION_NOT_FOUND));

		mockMvc.perform(get("/api/practice/sessions/" + SESSION_ID).cookie(userCookie))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRACTICE_SESSION_NOT_FOUND"));
	}

	@Test
	void recordAiResultWithEmptyBodyReturns400() throws Exception {
		mockMvc.perform(put("/api/practice/sessions/" + SESSION_ID + "/answers/" + QUESTION_ID)
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest());

		verify(practiceHistoryCommandService, never()).recordAiResult(any(), any(), any(), any(), any());
	}

	@Test
	void deleteReturns204() throws Exception {
		mockMvc.perform(delete("/api/practice/sessions/" + SESSION_ID).cookie(userCookie))
			.andExpect(status().isNoContent());

		verify(practiceHistoryCommandService).delete(any(), eq(UUID.fromString(SESSION_ID)));
	}

	@Test
	void deleteOtherUsersSessionReturns404() throws Exception {
		doThrow(new ApiException(ErrorCode.PRACTICE_SESSION_NOT_FOUND))
			.when(practiceHistoryCommandService).delete(any(), any());

		mockMvc.perform(delete("/api/practice/sessions/" + SESSION_ID).cookie(userCookie))
			.andExpect(status().isNotFound());
	}
}
