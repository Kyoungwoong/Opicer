package com.opicer.api.topic.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.domain.TopicBadge;
import com.opicer.api.topic.infrastructure.TopicRepository;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TopicControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@Autowired
	private TopicRepository topicRepository;

	private jakarta.servlet.http.Cookie userCookie;

	@BeforeEach
	void setUp() {
		topicRepository.deleteAll();
		userCookie = new jakarta.servlet.http.Cookie(authProperties.getCookieName(), jwtService.issueToken(
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
	void listReturnsActiveTopicsOnly() throws Exception {
		Topic active = new Topic("음악 감상", "Music", "여가/일상", 1, 1,
			List.of(new TopicBadge("일반 빈출", 17)), true);
		Topic inactive = new Topic("운동 안함", "No Exercise", "운동/건강", 2, 1,
			List.of(), false);
		topicRepository.saveAll(List.of(active, inactive));

		mockMvc.perform(get("/api/topics").cookie(userCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].title").value("음악 감상"));
	}
}
