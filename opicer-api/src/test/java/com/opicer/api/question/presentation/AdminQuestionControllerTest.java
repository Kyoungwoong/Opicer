package com.opicer.api.question.presentation;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A question must land on a registered topic or a named mock-test set, so it is always reachable. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminQuestionControllerTest {

	private static final String BASE = "/api/admin/questions";

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
	void aQuestionOnARegisteredTopicIsCreated() throws Exception {
		String title = "박물관 " + UUID.randomUUID().toString().substring(0, 8);
		topicRepository.save(new Topic(title, "Museum", Topic.CATEGORY_SURVEY, 1, 1, List.of(), true));

		mockMvc.perform(post(BASE).cookie(admin()).contentType(MediaType.APPLICATION_JSON).content(body(" " + title)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.topic").value(title));
	}

	@ParameterizedTest
	@ValueSource(strings = {"롤플레이: 카페 예약", "고난도: 기술 변화"})
	void aNamedMockTestSetIsAccepted(String topic) throws Exception {
		mockMvc.perform(post(BASE).cookie(admin()).contentType(MediaType.APPLICATION_JSON).content(body(topic)))
			.andExpect(status().isCreated());
	}

	@ParameterizedTest
	@ValueSource(strings = {"없는 주제 xyz", "롤플레이:", "고난도:   "})
	void anUnknownTopicOrAnUnnamedSetIs400(String topic) throws Exception {
		mockMvc.perform(post(BASE).cookie(admin()).contentType(MediaType.APPLICATION_JSON).content(body(topic)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	private String body(String topic) throws Exception {
		return json.writeValueAsString(Map.of(
			"topic", topic,
			"type", "DESCRIPTION",
			"promptText", "Tell me about it.",
			"targetLevels", List.of("IM"),
			"keyExpressions", List.of()));
	}

	private Cookie admin() {
		return new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(UUID.randomUUID(), "a@example.com", "Admin", UserRole.ADMIN, AuthProvider.KAKAO)));
	}
}
