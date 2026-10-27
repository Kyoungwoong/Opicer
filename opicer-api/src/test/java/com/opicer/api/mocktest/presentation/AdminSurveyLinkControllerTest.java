package com.opicer.api.mocktest.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The survey link screen's API: admin only, survey-category topics only, known items only. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminSurveyLinkControllerTest {

	private static final String BASE = "/api/admin/survey-links";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@Autowired
	private TopicRepository topicRepository;

	private final ObjectMapper json = new ObjectMapper();

	@Test
	void onlyAnAdminCanReadOrChangeLinks() throws Exception {
		mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(BASE).cookie(cookie(UserRole.USER))).andExpect(status().isForbidden());
		mockMvc.perform(put(BASE + "/leisure.museum").cookie(cookie(UserRole.USER))
				.contentType(MediaType.APPLICATION_JSON).content(body(List.of())))
			.andExpect(status().isForbidden());
		mockMvc.perform(get(BASE).cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].code").value("OCCUPATION"))
			.andExpect(jsonPath("$.data[3].items[?(@.id == 'leisure.museum')].label").value("박물관 가기"));
	}

	@Test
	void anItemsLinksAreReplacedAndListed() throws Exception {
		Topic museum = survey("박물관");

		mockMvc.perform(put(BASE + "/leisure.museum").cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON).content(body(List.of(museum.getId()))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value("leisure.museum"))
			.andExpect(jsonPath("$.data.topics[0].title").value(museum.getTitle()))
			.andExpect(jsonPath("$.data.topics[0].active").value(true));
		mockMvc.perform(get(BASE).cookie(cookie(UserRole.ADMIN)))
			.andExpect(jsonPath("$.data[3].items[?(@.id == 'leisure.museum')].topics[0].id").value(museum.getId().toString()));
		mockMvc.perform(get("/api/admin/topics").cookie(cookie(UserRole.ADMIN)))
			.andExpect(jsonPath("$.data[?(@.id == '" + museum.getId() + "')].mockLinked").value(true))
			.andExpect(jsonPath("$.data[?(@.id == '" + museum.getId() + "')].titleLocked").value(false));
	}

	@Test
	void anUnknownItemIs404AndAnUnknownOrNonSurveyTopicIs400() throws Exception {
		Topic unexpected = topicRepository.save(new Topic(unique("정전"), "Blackout", Topic.CATEGORY_UNEXPECTED, 2, 1,
			List.of(), true));

		mockMvc.perform(put(BASE + "/leisure.knitting").cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON).content(body(List.of())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("SURVEY_ITEM_NOT_FOUND"));
		mockMvc.perform(put(BASE + "/leisure.games").cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON).content(body(List.of(unexpected.getId()))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(put(BASE + "/leisure.games").cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON).content(body(List.of(UUID.randomUUID()))))
			.andExpect(status().isBadRequest());
		mockMvc.perform(put(BASE + "/leisure.games").cookie(cookie(UserRole.ADMIN))
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());
	}

	private Topic survey(String title) {
		return topicRepository.save(new Topic(unique(title), title, Topic.CATEGORY_SURVEY, 1, 1, List.of(), true));
	}

	private String body(List<UUID> topicIds) throws Exception {
		return json.writeValueAsString(Map.of("topicIds", topicIds));
	}

	private static String unique(String title) {
		return title + " " + UUID.randomUUID().toString().substring(0, 8);
	}

	private Cookie cookie(UserRole role) {
		return new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(UUID.randomUUID(), "a@example.com", "Admin", role, AuthProvider.KAKAO)));
	}
}
