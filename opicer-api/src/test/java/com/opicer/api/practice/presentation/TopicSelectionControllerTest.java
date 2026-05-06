package com.opicer.api.practice.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.credit.domain.CreditBalance;
import com.opicer.api.credit.infrastructure.CreditBalanceRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TopicSelectionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private CreditBalanceRepository creditBalanceRepository;

	private jakarta.servlet.http.Cookie userCookie;
	private UUID userId;

	@BeforeEach
	void setUp() {
		topicRepository.deleteAll();
		creditBalanceRepository.deleteAll();
		userId = UUID.randomUUID();
		userCookie = new jakarta.servlet.http.Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(
				userId,
				"user@example.com",
				"User",
				UserRole.USER,
				AuthProvider.KAKAO
			)
		));
		CreditBalance balance = new CreditBalance(userId);
		balance.increase(10);
		creditBalanceRepository.save(balance);
	}

	@Test
	void createSelectionStoresTopic() throws Exception {
		Topic topic = new Topic("음악 감상", "Music", "여가/일상", 1, 1, List.of(), true);
		topic = topicRepository.save(topic);

		mockMvc.perform(post("/api/practice/topic-selections")
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "topicId": "%s"
					}
					""".formatted(topic.getId())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.topicId").value(topic.getId().toString()));
	}

	@Test
	void inactiveTopicReturnsErrorCode() throws Exception {
		Topic topic = new Topic("운동 안함", "No Exercise", "운동/건강", 1, 1, List.of(), false);
		topic = topicRepository.save(topic);

		mockMvc.perform(post("/api/practice/topic-selections")
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "topicId": "%s"
					}
					""".formatted(topic.getId())))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TOPIC_INACTIVE"));
	}

	@Test
	void createSelectionWithoutEnoughCreditReturns402() throws Exception {
		creditBalanceRepository.deleteAll();
		CreditBalance lowBalance = new CreditBalance(userId);
		lowBalance.increase(1);
		creditBalanceRepository.save(lowBalance);
		Topic topic = new Topic("음악 감상", "Music", "설문조사", 1, 1, List.of(), true);
		topic = topicRepository.save(topic);

		mockMvc.perform(post("/api/practice/topic-selections")
				.cookie(userCookie)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "topicId": "%s"
					}
					""".formatted(topic.getId())))
			.andExpect(status().isPaymentRequired())
			.andExpect(jsonPath("$.code").value("CREDIT_INSUFFICIENT_BALANCE"));
	}
}
