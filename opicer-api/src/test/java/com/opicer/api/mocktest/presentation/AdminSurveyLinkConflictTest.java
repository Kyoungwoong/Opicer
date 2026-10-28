package com.opicer.api.mocktest.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.auth.application.JwtService;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.config.AuthProperties;
import com.opicer.api.mocktest.domain.SurveyTopicLink;
import com.opicer.api.mocktest.infrastructure.SurveyTopicLinkRepository;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.UserRole;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two admins saving the same item at once can both insert one (item, topic) pair. The insert is flushed when the
 * transaction commits, so the unique key fails there; that must still be a 409, not a 500. Skipping the delete
 * replays the race on real Postgres without threads.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminSurveyLinkConflictTest {

	private static final String ITEM = "leisure.billiards";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthProperties authProperties;

	@Autowired
	private TopicRepository topicRepository;

	@SpyBean
	private SurveyTopicLinkRepository links;

	private final ObjectMapper json = new ObjectMapper();

	@Test
	void aDuplicatePairFailingAtCommitIs409() throws Exception {
		Topic billiards = topicRepository.save(new Topic("당구 " + UUID.randomUUID().toString().substring(0, 8),
			"Billiards", Topic.CATEGORY_SURVEY, 1, 1, List.of(), true));
		List<SurveyTopicLink> before = links.findByItemIdIn(List.of(ITEM));
		try {
			mockMvc.perform(replace(billiards.getId())).andExpect(status().isOk());
			doReturn(0).when(links).deleteByItemId(ITEM);

			mockMvc.perform(replace(billiards.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DATA_INTEGRITY_VIOLATION"));
		} finally {
			Mockito.reset(links);
			links.deleteAll(links.findByItemIdIn(List.of(ITEM)));
			links.saveAll(before.stream().map(link -> new SurveyTopicLink(link.getItemId(), link.getTopicId())).toList());
			topicRepository.deleteById(billiards.getId());
		}
	}

	private MockHttpServletRequestBuilder replace(UUID topicId) throws Exception {
		return put("/api/admin/survey-links/" + ITEM).cookie(adminCookie())
			.contentType(MediaType.APPLICATION_JSON)
			.content(json.writeValueAsString(Map.of("topicIds", List.of(topicId))));
	}

	private Cookie adminCookie() {
		return new Cookie(authProperties.getCookieName(), jwtService.issueToken(
			new AuthUserPrincipal(UUID.randomUUID(), "a@example.com", "Admin", UserRole.ADMIN, AuthProvider.KAKAO)));
	}
}
