package com.opicer.api.opic.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.opic.application.OpicQueryService;
import com.opicer.api.opic.application.OpicSyncService;
import com.opicer.api.opic.application.OpicSyncWriter;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import jakarta.servlet.http.Cookie;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpicControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@MockBean
	private OpicQueryService opicQueryService;

	@MockBean
	private OpicSyncService opicSyncService;

	@Test
	void schedulesArePublic() throws Exception {
		when(opicQueryService.schedules(YearMonth.of(2026, 11))).thenReturn(List.of());

		mockMvc.perform(get("/api/opic/schedules").param("month", "2026-11"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.month").value("2026-11"))
			.andExpect(jsonPath("$.data.source").value("opic.or.kr"));
	}

	@Test
	void invalidMonthReturns400() throws Exception {
		mockMvc.perform(get("/api/opic/schedules").param("month", "2026/11"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void centersArePublic() throws Exception {
		when(opicQueryService.centers(any())).thenReturn(List.of());

		mockMvc.perform(get("/api/opic/centers").param("region", "서울"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	void adminSyncRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/admin/opic-sync"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void adminSyncRejectsNonAdmin() throws Exception {
		mockMvc.perform(post("/api/admin/opic-sync").cookie(cookie(UserRole.USER)))
			.andExpect(status().isForbidden());
		verify(opicSyncService, never()).syncSchedules();
	}

	@Test
	void adminSyncRunsBothTargets() throws Exception {
		when(opicSyncService.syncSchedules()).thenReturn(28);
		when(opicSyncService.syncCenters()).thenReturn(new OpicSyncWriter.CenterSyncResult(45, 1));

		mockMvc.perform(post("/api/admin/opic-sync").cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.schedulesUpserted").value(28))
			.andExpect(jsonPath("$.data.centersUpserted").value(45))
			.andExpect(jsonPath("$.data.centersDeactivated").value(1));
	}

	@Test
	void adminSyncRejectsUnknownTarget() throws Exception {
		mockMvc.perform(post("/api/admin/opic-sync").param("target", "reviews").cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isBadRequest());
	}

	private Cookie cookie(UserRole role) {
		return new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(UUID.randomUUID(), "u@example.com", "User", role, AuthProvider.KAKAO)
		));
	}
}
