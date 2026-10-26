package com.opicer.api.evaluationguide.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.evaluation.domain.EvaluationGuideKind;
import com.opicer.api.evaluationguide.application.EvaluationGuideCommandService;
import com.opicer.api.evaluationguide.application.EvaluationGuideQueryService;
import com.opicer.api.evaluationguide.domain.EvaluationGuide;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
class AdminEvaluationGuideControllerTest {

	private static final String BASE = "/api/admin/evaluation-guides";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@MockBean
	private EvaluationGuideCommandService commandService;

	@MockBean
	private EvaluationGuideQueryService queryService;

	@Test
	void listWithoutCredentialsIs401() throws Exception {
		mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		verifyNoInteractions(queryService);
	}

	@Test
	void listAsLearnerIs403() throws Exception {
		mockMvc.perform(get(BASE).cookie(cookie(UserRole.USER))).andExpect(status().isForbidden());
		verifyNoInteractions(queryService);
	}

	@Test
	void listAsAdminReturnsGuides() throws Exception {
		when(queryService.findAll()).thenReturn(List.of(guide(EvaluationGuideKind.TIPS, 2, true)));

		mockMvc.perform(get(BASE).cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].kind").value("TIPS"))
			.andExpect(jsonPath("$.data[0].kindLabel").value("코칭 팁"))
			.andExpect(jsonPath("$.data[0].version").value(2))
			.andExpect(jsonPath("$.data[0].active").value(true));
	}

	@Test
	void createAsAdminReturns201() throws Exception {
		when(commandService.create(eq(EvaluationGuideKind.REPORT), eq("문체"), anyString()))
			.thenReturn(guide(EvaluationGuideKind.REPORT, 1, false));

		mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"kind\":\"REPORT\",\"title\":\"문체\",\"contentMd\":\"- 존댓말\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.version").value(1));
	}

	@Test
	void createAsLearnerIs403() throws Exception {
		mockMvc.perform(post(BASE).cookie(cookie(UserRole.USER))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"kind\":\"REPORT\",\"title\":\"문체\",\"contentMd\":\"- 존댓말\"}"))
			.andExpect(status().isForbidden());
		verifyNoInteractions(commandService);
	}

	@Test
	void contentOverTheLimitIs400() throws Exception {
		String tooLong = "a".repeat(EvaluationGuide.MAX_CONTENT_LENGTH + 1);

		mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"kind\":\"TIPS\",\"title\":\"팁\",\"contentMd\":\"" + tooLong + "\"}"))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(commandService);
	}

	@Test
	void editingAnActiveGuideIs409() throws Exception {
		UUID id = UUID.randomUUID();
		when(commandService.update(eq(id), any(), any())).thenThrow(new ApiException(ErrorCode.EVALUATION_GUIDE_ACTIVE));

		mockMvc.perform(put(BASE + "/" + id).cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"팁\",\"contentMd\":\"changed\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EVALUATION_GUIDE_ACTIVE"));
	}

	@Test
	void activateAndDeactivateAsAdmin() throws Exception {
		UUID id = UUID.randomUUID();
		when(commandService.activate(id)).thenReturn(guide(EvaluationGuideKind.TIPS, 1, true));
		when(commandService.deactivate(id)).thenReturn(guide(EvaluationGuideKind.TIPS, 1, false));

		mockMvc.perform(post(BASE + "/" + id + "/activate").cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.active").value(true));
		mockMvc.perform(post(BASE + "/" + id + "/deactivate").cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.active").value(false));
	}

	@Test
	void deleteUnknownGuideIs404() throws Exception {
		UUID id = UUID.randomUUID();
		org.mockito.Mockito.doThrow(new ApiException(ErrorCode.EVALUATION_GUIDE_NOT_FOUND)).when(commandService).delete(id);

		mockMvc.perform(delete(BASE + "/" + id).cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isNotFound());
	}

	private Cookie cookie(UserRole role) {
		return new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(UUID.randomUUID(), "user@example.com", "User", role, AuthProvider.KAKAO)));
	}

	private static EvaluationGuide guide(EvaluationGuideKind kind, int version, boolean active) {
		EvaluationGuide guide = new EvaluationGuide(kind, version, "제목", "- 내용");
		if (active) {
			guide.activate();
		}
		ReflectionTestUtils.setField(guide, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(guide, "createdAt", Instant.now());
		ReflectionTestUtils.setField(guide, "updatedAt", Instant.now());
		return guide;
	}
}
