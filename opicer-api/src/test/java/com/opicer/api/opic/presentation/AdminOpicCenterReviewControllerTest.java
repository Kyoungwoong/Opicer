package com.opicer.api.opic.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.opic.application.OpicCenterReviewCommandService;
import com.opicer.api.opic.application.OpicCenterReviewQueryService;
import com.opicer.api.opic.domain.OpicCenterReview;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Optional;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
class AdminOpicCenterReviewControllerTest {

	private static final String VALID_BODY = """
		{"rating": 4, "content": "주차가 편해요", "sourceLabel": "응시자 제보"}
		""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@MockBean
	private OpicCenterReviewQueryService reviewQueryService;

	@MockBean
	private OpicCenterReviewCommandService reviewCommandService;

	@Test
	void listRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/admin/opic-centers/{id}/reviews", UUID.randomUUID()))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void createRejectsNonAdmin() throws Exception {
		mockMvc.perform(post("/api/admin/opic-centers/{id}/reviews", UUID.randomUUID())
				.cookie(cookie(UserRole.USER))
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY))
			.andExpect(status().isForbidden());
		verify(reviewCommandService, never()).create(any(), anyInt(), any(), any(), anyBoolean());
	}

	@Test
	void listIncludesHiddenReviewsForAdmin() throws Exception {
		UUID centerId = UUID.randomUUID();
		when(reviewQueryService.allReviews(centerId)).thenReturn(List.of(review(centerId, 2, false)));

		mockMvc.perform(get("/api/admin/opic-centers/{id}/reviews", centerId).cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].visible").value(false))
			.andExpect(jsonPath("$.data[0].centerId").value(centerId.toString()));
	}

	@Test
	void createDefaultsToVisible() throws Exception {
		UUID centerId = UUID.randomUUID();
		when(reviewCommandService.create(centerId, 4, "주차가 편해요", "응시자 제보", true))
			.thenReturn(review(centerId, 4, true));

		mockMvc.perform(post("/api/admin/opic-centers/{id}/reviews", centerId)
				.cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.rating").value(4))
			.andExpect(jsonPath("$.data.visible").value(true));
	}

	@Test
	void createRejectsOutOfRangeRating() throws Exception {
		mockMvc.perform(post("/api/admin/opic-centers/{id}/reviews", UUID.randomUUID())
				.cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\": 6, \"content\": \"너무 좋아요\"}"))
			.andExpect(status().isBadRequest());
		verify(reviewCommandService, never()).create(any(), anyInt(), any(), any(), anyBoolean());
	}

	@Test
	void createRejectsBlankContent() throws Exception {
		mockMvc.perform(post("/api/admin/opic-centers/{id}/reviews", UUID.randomUUID())
				.cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\": 3, \"content\": \"  \"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void updateMissingReviewReturns404() throws Exception {
		UUID id = UUID.randomUUID();
		when(reviewCommandService.update(eq(id), anyInt(), any(), any(), anyBoolean())).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/admin/opic-center-reviews/{id}", id)
				.cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("OPIC_CENTER_REVIEW_NOT_FOUND"));
	}

	@Test
	void deleteReturns200ThenNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		when(reviewCommandService.delete(id)).thenReturn(true, false);

		mockMvc.perform(delete("/api/admin/opic-center-reviews/{id}", id).cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isOk());
		mockMvc.perform(delete("/api/admin/opic-center-reviews/{id}", id).cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isNotFound());
	}

	private static OpicCenterReview review(UUID centerId, int rating, boolean visible) {
		OpicCenterReview review = new OpicCenterReview(centerId, rating, "주차가 편해요", "응시자 제보", visible);
		ReflectionTestUtils.setField(review, "id", UUID.randomUUID());
		return review;
	}

	private Cookie cookie(UserRole role) {
		return new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(UUID.randomUUID(), "u@example.com", "User", role, AuthProvider.KAKAO)
		));
	}
}
