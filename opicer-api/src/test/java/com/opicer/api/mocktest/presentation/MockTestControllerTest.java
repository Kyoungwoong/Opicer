package com.opicer.api.mocktest.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.mocktest.application.MockTestCommandService;
import com.opicer.api.mocktest.application.MockTestQueryService;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MockTestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@MockBean
	private MockTestCommandService commandService;

	@MockBean
	private MockTestQueryService queryService;

	@Test
	void everyEndpointNeedsSignIn() throws Exception {
		mockMvc.perform(get("/api/mock-tests")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/mock-tests/survey")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/mock-tests").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnauthorized());
		verifyNoInteractions(commandService, queryService);
	}

	@Test
	void surveyCatalogIsServedWithTheRulesAndCost() throws Exception {
		mockMvc.perform(get("/api/mock-tests/survey").cookie(cookie()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.parts[0].code").value("OCCUPATION"))
			.andExpect(jsonPath("$.data.parts[3].min").value(2))
			.andExpect(jsonPath("$.data.parts[3].items[0].label").value("영화 보기"))
			.andExpect(jsonPath("$.data.part4MinTotal").value(12))
			.andExpect(jsonPath("$.data.cost").value(5));
	}

	@Test
	void selfAssessmentOutOfRangeIs400() throws Exception {
		mockMvc.perform(post("/api/mock-tests").cookie(cookie()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"survey\":{},\"selfAssessment\":7}"))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(commandService);
	}

	@Test
	void adjustingTwiceIs409() throws Exception {
		UUID id = UUID.randomUUID();
		when(commandService.adjust(any(), eq(id), any())).thenThrow(new ApiException(ErrorCode.MOCK_TEST_ALREADY_ADJUSTED));

		mockMvc.perform(post("/api/mock-tests/" + id + "/adjust").cookie(cookie()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"difficulty\":\"HARDER\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MOCK_TEST_ALREADY_ADJUSTED"));
	}

	@Test
	void someoneElsesTestIs404() throws Exception {
		UUID id = UUID.randomUUID();
		when(queryService.get(any(), eq(id))).thenThrow(new ApiException(ErrorCode.MOCK_TEST_NOT_FOUND));

		mockMvc.perform(get("/api/mock-tests/" + id).cookie(cookie()))
			.andExpect(status().isNotFound());
	}

	private Cookie cookie() {
		return new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(UUID.randomUUID(), "u@example.com", "User", UserRole.USER, AuthProvider.KAKAO)));
	}
}
