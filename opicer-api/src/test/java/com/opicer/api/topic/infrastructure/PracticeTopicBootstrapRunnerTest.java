package com.opicer.api.topic.infrastructure;

import com.opicer.api.question.domain.Question;
import com.opicer.api.question.infrastructure.QuestionRepository;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.domain.TopicBadge;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticeTopicBootstrapRunnerTest {

	@Mock
	private TopicRepository topicRepository;

	@Mock
	private QuestionRepository questionRepository;

	@InjectMocks
	private PracticeTopicBootstrapRunner runner;

	@Test
	void run_reclassifiesLegacyCategories_andSeedsSelfIntro() throws Exception {
		Topic surveyTopic = new Topic(
			"음악 감상",
			"Music",
			"여가/일상",
			1,
			0,
			List.of(new TopicBadge("빈출", null)),
			true
		);
		Topic unexpectedTopic = new Topic(
			"물건 분실",
			"Lost Item",
			"돌발/상황 대처",
			5,
			1,
			List.of(new TopicBadge("돌발 빈출", null)),
			true
		);

		when(topicRepository.findAll()).thenReturn(List.of(surveyTopic, unexpectedTopic));
		when(topicRepository.findByTitle("자기소개")).thenReturn(Optional.empty());
		when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(questionRepository.findByActiveTrueAndTopicOrderByCreatedAtAscIdAsc("자기소개"))
			.thenReturn(List.of());

		runner.run(new DefaultApplicationArguments(new String[] {}));

		assertThat(surveyTopic.getCategory()).isEqualTo("설문조사");
		assertThat(surveyTopic.getCategoryOrder()).isEqualTo(1);
		assertThat(unexpectedTopic.getCategory()).isEqualTo("돌발질문");
		assertThat(unexpectedTopic.getCategoryOrder()).isEqualTo(2);
		verify(questionRepository, times(1)).saveAll(any());
	}

	@Test
	void run_skipsQuestionSeeding_whenSelfIntroQuestionsAlreadyExist() throws Exception {
		Topic selfIntro = new Topic(
			"자기소개",
			"Self Introduction",
			"자기소개",
			0,
			0,
			List.of(new TopicBadge("필수", null)),
			true
		);
		Topic unexpectedTopic = new Topic(
			"약속 취소",
			"Cancelled Plans",
			"돌발질문",
			2,
			0,
			List.of(new TopicBadge("돌발 빈출", null)),
			true
		);

		when(topicRepository.findAll()).thenReturn(List.of(selfIntro, unexpectedTopic));
		when(topicRepository.findByTitle("자기소개")).thenReturn(Optional.of(selfIntro));
		when(questionRepository.findByActiveTrueAndTopicOrderByCreatedAtAscIdAsc("자기소개"))
			.thenReturn(List.of(mock(Question.class)));

		runner.run(new DefaultApplicationArguments(new String[] {}));

		verify(questionRepository, never()).saveAll(any());
		verify(topicRepository, never()).save(eq(selfIntro));
	}
}
