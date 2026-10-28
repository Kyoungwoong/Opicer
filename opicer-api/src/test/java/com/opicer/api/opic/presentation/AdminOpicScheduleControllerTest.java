package com.opicer.api.opic.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.opic.application.OpicScheduleCommandService;
import com.opicer.api.opic.domain.OpicExamSchedule;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminOpicScheduleControllerTest {

	private static final String BASE = "/api/admin/opic-schedules";
	private static final String BODY = """
		{"examDate":"2026-11-08","registrationStart":"2026-10-20","registrationEnd":"2026-11-03",
		 "scoreReleaseDate":"2026-11-13","roundName":"정기 평가","earlyRelease":true}""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@MockBean
	private OpicScheduleCommandService commandService;

	@Test
	void listWithoutCredentialsIs401() throws Exception {
		mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		verifyNoInteractions(commandService);
	}

	@Test
	void createAsLearnerIs403() throws Exception {
		mockMvc.perform(post(BASE).cookie(cookie(UserRole.USER)).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isForbidden());
		verifyNoInteractions(commandService);
	}

	@Test
	void listAsAdminShowsTheSource() throws Exception {
		when(commandService.findAll()).thenReturn(List.of(schedule()));

		mockMvc.perform(get(BASE).cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].examDate").value("2026-11-08"))
			.andExpect(jsonPath("$.data[0].roundName").value("정기 평가"))
			.andExpect(jsonPath("$.data[0].source").value("manual"));
	}

	@Test
	void createAsAdminIs201AndPassesTheDates() throws Exception {
		when(commandService.create(any())).thenReturn(schedule());

		mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.earlyRelease").value(true));
		verify(commandService).create(argThat(input -> input.examDate().equals(LocalDate.of(2026, 11, 8))
			&& input.registrationEnd().equals(LocalDate.of(2026, 11, 3)) && input.earlyRelease()));
	}

	@Test
	void missingExamDateOrBlankRoundNameIs400() throws Exception {
		mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"roundName\":\"정기 평가\"}"))
			.andExpect(status().isBadRequest());
		mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"examDate\":\"2026-11-08\",\"roundName\":\" \"}"))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(commandService);
	}

	@Test
	void duplicateIs409() throws Exception {
		UUID id = UUID.randomUUID();
		when(commandService.update(eq(id), any())).thenThrow(new ApiException(ErrorCode.OPIC_SCHEDULE_DUPLICATE));

		mockMvc.perform(put(BASE + "/" + id).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("OPIC_SCHEDULE_DUPLICATE"));
	}

	@Test
	void deletingAMissingScheduleIs404() throws Exception {
		UUID id = UUID.randomUUID();
		doThrow(new ApiException(ErrorCode.OPIC_SCHEDULE_NOT_FOUND)).when(commandService).delete(id);

		mockMvc.perform(delete(BASE + "/" + id).cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("OPIC_SCHEDULE_NOT_FOUND"));
	}

	private Cookie cookie(UserRole role) {
		return new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(UUID.randomUUID(), "u@example.com", "User", role, AuthProvider.KAKAO)));
	}

	private static OpicExamSchedule schedule() {
		OpicExamSchedule schedule = new OpicExamSchedule(LocalDate.of(2026, 11, 8), LocalDate.of(2026, 10, 20),
			LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 13), "정기 평가", true, OpicExamSchedule.SOURCE_MANUAL);
		ReflectionTestUtils.setField(schedule, "id", UUID.randomUUID());
		return schedule;
	}
}
