package com.opicer.api.topic.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.practice.domain.TopicSelection;
import com.opicer.api.practice.infrastructure.TopicSelectionRepository;
import com.opicer.api.question.domain.Question;
import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.question.infrastructure.QuestionRepository;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.topic.infrastructure.TopicRepository;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.User;
import com.opicer.api.user.domain.UserRole;
import com.opicer.api.user.infrastructure.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Topic rules end to end on real Postgres: titles, categories, renames and delete protection. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminTopicControllerTest {

	private static final String BASE = "/api/admin/topics";

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

	@Autowired
	private TopicSelectionRepository topicSelectionRepository;

	@Autowired
	private UserRepository userRepository;

	private final ObjectMapper json = new ObjectMapper();

	@Test
	void writesNeedAnAdmin() throws Exception {
		mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
		mockMvc.perform(post(BASE).cookie(cookie(UserRole.USER)).contentType(MediaType.APPLICATION_JSON)
				.content(body(unique("박물관"), "설문조사")))
			.andExpect(status().isForbidden());
		mockMvc.perform(get(BASE).cookie(cookie(UserRole.ADMIN))).andExpect(status().isOk());
	}

	@Test
	void createTrimsTheTitleAndRejectsADuplicate() throws Exception {
		String title = unique("박물관");

		mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON)
				.content(body("  " + title + "  ", "설문조사")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.title").value(title));
		mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON)
				.content(body(title, "돌발질문")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TOPIC_TITLE_DUPLICATE"));
	}

	@Test
	void anUnknownCategoryOrAReservedSetNameIs400() throws Exception {
		mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON)
				.content(body(unique("게임"), "취미")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON)
				.content(body("롤플레이: " + unique("카페"), "돌발질문")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void renamingATopicMovesItsQuestionsAlong() throws Exception {
		String before = unique("게임");
		String after = unique("온라인 게임");
		UUID id = create(before, "설문조사");
		Question question = questionRepository.save(new Question(before, QuestionType.DESCRIPTION, "Tell me about games.",
			null, null, List.of(OpicLevel.IM), List.of()));

		mockMvc.perform(put(BASE + "/" + id).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON)
				.content(body(after, "설문조사")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").value(after));

		assertThat(questionRepository.findById(question.getId()).orElseThrow().getTopic()).isEqualTo(after);
		assertThat(questionRepository.existsByTopic(before)).isFalse();
	}

	@Test
	void keepingTheSameTitleIsNotADuplicateOfItself() throws Exception {
		String title = unique("SNS");
		UUID id = create(title, "설문조사");

		mockMvc.perform(put(BASE + "/" + id).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON)
				.content(body(title, "돌발질문")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.category").value("돌발질문"));
	}

	@Test
	void aTopicWithQuestionsOrPracticeHistoryCannotBeDeleted() throws Exception {
		UUID withQuestion = create(unique("요가"), "설문조사");
		questionRepository.save(new Question(topicRepository.findById(withQuestion).orElseThrow().getTitle(),
			QuestionType.DESCRIPTION, "Tell me about yoga.", null, null, List.of(OpicLevel.IM), List.of()));
		UUID practised = create(unique("등산"), "설문조사");
		topicSelectionRepository.save(new TopicSelection(user(), practised));

		mockMvc.perform(delete(BASE + "/" + withQuestion).cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("TOPIC_IN_USE"));
		mockMvc.perform(delete(BASE + "/" + practised).cookie(cookie(UserRole.ADMIN)))
			.andExpect(status().isConflict());
		assertThat(topicRepository.existsById(withQuestion)).isTrue();
		assertThat(topicRepository.existsById(practised)).isTrue();
	}

	@Test
	void anUnusedTopicIsDeleted() throws Exception {
		UUID id = create(unique("낚시"), "돌발질문");

		mockMvc.perform(delete(BASE + "/" + id).cookie(cookie(UserRole.ADMIN))).andExpect(status().isOk());

		assertThat(topicRepository.existsById(id)).isFalse();
	}

	private UUID create(String title, String category) throws Exception {
		String response = mockMvc.perform(post(BASE).cookie(cookie(UserRole.ADMIN)).contentType(MediaType.APPLICATION_JSON)
				.content(body(title, category)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		JsonNode created = json.readTree(response);
		return UUID.fromString(created.path("data").path("id").asText());
	}

	private String body(String title, String category) throws Exception {
		return json.writeValueAsString(java.util.Map.of(
			"title", title,
			"englishTitle", "Topic",
			"category", category,
			"categoryOrder", 1,
			"topicOrder", 1,
			"badges", List.of(),
			"active", true));
	}

	private static String unique(String title) {
		return title + " " + UUID.randomUUID().toString().substring(0, 8);
	}

	private UUID user() {
		return userRepository.save(new User(AuthProvider.KAKAO, "topic-" + UUID.randomUUID(), "t@example.com", "topic-user",
			UserRole.USER)).getId();
	}

	private Cookie cookie(UserRole role) {
		return new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(UUID.randomUUID(), "a@example.com", "Admin", role, AuthProvider.KAKAO)));
	}
}
