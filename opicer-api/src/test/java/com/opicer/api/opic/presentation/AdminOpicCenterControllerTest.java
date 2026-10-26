package com.opicer.api.opic.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.opic.application.OpicCenterCommandService;
import com.opicer.api.opic.application.OpicCenterCommandService.Saved;
import com.opicer.api.opic.domain.OpicTestCenter;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminOpicCenterControllerTest {

	private static final String BASE = "/api/admin/opic-centers";
	private static final String BODY = "{\"name\":\"강남센터\",\"address\":\"서울 강남구 테헤란로 123\",\"regionName\":\"서울\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@MockBean
	private OpicCenterCommandService commandService;

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
	void listAsAdminIncludesInactiveCenters() throws Exception {
		OpicTestCenter inactive = center();
		inactive.deactivate();
		when(commandService.findAll()).thenReturn(List.of(inactive));

		mockMvc.perform(get(BASE).cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].name").value("강남센터"))
			.andExpect(jsonPath("$.data[0].active").value(false));
	}

	@Test
	void createReturns201WithTheGeocodeWarning() throws Exception {
		when(commandService.create(any())).thenReturn(new Saved(center(), "키가 없어요"));

		mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.geocodeWarning").value("키가 없어요"))
			.andExpect(jsonPath("$.data.latitude").doesNotExist());
	}

	@Test
	void blankNameIs400() throws Exception {
		mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\" \",\"address\":\"서울\"}"))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(commandService);
	}

	@Test
	void duplicateIs409() throws Exception {
		UUID id = UUID.randomUUID();
		when(commandService.update(eq(id), any())).thenThrow(new ApiException(ErrorCode.OPIC_CENTER_DUPLICATE));

		mockMvc.perform(put(BASE + "/" + id).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("OPIC_CENTER_DUPLICATE"));
	}

	@Test
	void deactivateAsAdmin() throws Exception {
		UUID id = UUID.randomUUID();
		OpicTestCenter center = center();
		center.deactivate();
		when(commandService.setActive(id, false)).thenReturn(center);

		mockMvc.perform(post(BASE + "/" + id + "/deactivate").cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.active").value(false));
	}

	private Cookie cookie(UserRole role) {
		return new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(UUID.randomUUID(), "u@example.com", "User", role, AuthProvider.KAKAO)));
	}

	private static OpicTestCenter center() {
		OpicTestCenter center = new OpicTestCenter("강남센터", "서울 강남구 테헤란로 123", null, null, null, "서울", Instant.now());
		ReflectionTestUtils.setField(center, "id", UUID.randomUUID());
		return center;
	}
}
