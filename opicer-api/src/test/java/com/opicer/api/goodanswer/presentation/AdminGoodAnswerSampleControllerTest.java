package com.opicer.api.goodanswer.presentation;

import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.goodanswer.application.GoodAnswerSampleService;
import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminGoodAnswerSampleControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@MockBean
	private GoodAnswerSampleService service;

	private Cookie adminCookie;
	private UUID topicId;

	@BeforeEach
	void setUp() {
		adminCookie = new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(
				UUID.randomUUID(),
				"admin@example.com",
				"Admin",
				UserRole.ADMIN,
				AuthProvider.KAKAO
			)
		));
		topicId = UUID.randomUUID();
	}

	@Test
	void create_validRequest_returns200() throws Exception {
		Topic topic = new Topic("title", "english", "cat", 1, 1, List.of(), true);
		ReflectionTestUtils.setField(topic, "id", topicId);
		GoodAnswerSample sample = new GoodAnswerSample(
			topic,
			OpicLevel.IM,
			"sample text",
			"https://example.com/audio.mp3",
			"summary",
			List.of("tag1"),
			List.of("expr1"),
			new float[] {0.1f, 0.2f}
		);
		ReflectionTestUtils.setField(sample, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(sample, "createdAt", java.time.Instant.now());
		ReflectionTestUtils.setField(sample, "updatedAt", java.time.Instant.now());

		when(service.create(eq(topicId), eq(OpicLevel.IM), anyString(), any(), any(), anyList(), anyList()))
			.thenReturn(sample);

		String payload = """
			{
			  "topicId": "%s",
			  "level": "IM",
			  "sampleText": "sample text",
			  "sampleAudioUrl": "https://example.com/audio.mp3",
			  "summary": "summary",
			  "tags": ["tag1"],
			  "keyExpressions": ["expr1"]
			}
			""".formatted(topicId);

		mockMvc.perform(post("/api/admin/good-answers")
				.cookie(adminCookie)
				.contentType("application/json")
				.content(payload))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").exists());
	}

	@Test
	void list_byTopic_returns200() throws Exception {
		Topic topic = new Topic("title", "english", "cat", 1, 1, List.of(), true);
		ReflectionTestUtils.setField(topic, "id", topicId);
		GoodAnswerSample sample = new GoodAnswerSample(
			topic,
			OpicLevel.AL,
			"sample text",
			null,
			null,
			List.of(),
			List.of(),
			new float[] {0.1f}
		);
		ReflectionTestUtils.setField(sample, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(sample, "createdAt", java.time.Instant.now());
		ReflectionTestUtils.setField(sample, "updatedAt", java.time.Instant.now());
		when(service.listByTopic(topicId)).thenReturn(List.of(sample));

		mockMvc.perform(get("/api/admin/good-answers")
				.cookie(adminCookie)
				.param("topicId", topicId.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].topicId").value(topicId.toString()));
	}

	@Test
	void uploadAudio_validRequest_returns200() throws Exception {
		Topic topic = new Topic("title", "english", "cat", 1, 1, List.of(), true);
		ReflectionTestUtils.setField(topic, "id", topicId);
		GoodAnswerSample sample = new GoodAnswerSample(
			topic,
			OpicLevel.IM,
			"sample text",
			"/media/good-answers/sample.webm",
			"summary",
			List.of("tag1"),
			List.of("expr1"),
			new float[] {0.1f}
		);
		ReflectionTestUtils.setField(sample, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(sample, "createdAt", java.time.Instant.now());
		ReflectionTestUtils.setField(sample, "updatedAt", java.time.Instant.now());

		when(service.createFromAudio(eq(topicId), eq(OpicLevel.IM), any(), any(), anyList(), anyList()))
			.thenReturn(List.of(sample));

		MockMultipartFile file = new MockMultipartFile(
			"audio",
			"sample.webm",
			"audio/webm",
			"dummy".getBytes()
		);

		mockMvc.perform(multipart("/api/admin/good-answers/audio")
				.file(file)
				.param("topicId", topicId.toString())
				.param("level", "IM")
				.param("summary", "summary")
				.param("tags", "tag1")
				.param("keyExpressions", "expr1")
				.cookie(adminCookie))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].id").exists())
			.andExpect(jsonPath("$.data[0].sampleText").value("sample text"))
			.andExpect(jsonPath("$.data[0].audioUrl").value("/media/good-answers/sample.webm"));
	}

	@Test
	void uploadAudio_missingFile_returns400() throws Exception {
		mockMvc.perform(multipart("/api/admin/good-answers/audio")
				.param("topicId", topicId.toString())
				.param("level", "IM")
				.cookie(adminCookie))
			.andExpect(status().isBadRequest());
	}

	@Test
	void uploadAudio_transcriptionFailed_returns502() throws Exception {
		when(service.createFromAudio(eq(topicId), eq(OpicLevel.IM), any(), any(), anyList(), anyList()))
			.thenThrow(new ApiException(ErrorCode.AI_TRANSCRIPTION_FAILED));

		MockMultipartFile file = new MockMultipartFile(
			"audio",
			"sample.webm",
			"audio/webm",
			"dummy".getBytes()
		);

		mockMvc.perform(multipart("/api/admin/good-answers/audio")
				.file(file)
				.param("topicId", topicId.toString())
				.param("level", "IM")
				.cookie(adminCookie))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.code").value("AI_TRANSCRIPTION_FAILED"));
	}
}
