package com.opicer.api.practice.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.question.domain.Question;
import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.question.infrastructure.QuestionRepository;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.topic.domain.Topic;
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
class PracticeQuestionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private QuestionRepository questionRepository;

	private jakarta.servlet.http.Cookie userCookie;

	@BeforeEach
	void setUp() {
		questionRepository.deleteAll();
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
	void listReturnsActiveQuestionsByTopic() throws Exception {
		Topic topic = new Topic("음악 감상", "Music", "여가/일상", 1, 1, List.of(), true);
		topic = topicRepository.save(topic);

		Question q1 = new Question("음악 감상", QuestionType.DESCRIPTION, "Describe your favorite music.",
			null, null, List.of(OpicLevel.IM), List.of("favorite"));
		Question q2 = new Question("음악 감상", QuestionType.OPINION, "Why do you like it?",
			null, null, List.of(OpicLevel.IM), List.of("reason"));
		questionRepository.saveAll(List.of(q1, q2));

		mockMvc.perform(get("/api/practice/topics/{id}/questions", topic.getId()).cookie(userCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].topic").value("음악 감상"));
	}
}
