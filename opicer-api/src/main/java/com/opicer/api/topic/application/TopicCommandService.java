package com.opicer.api.topic.application;

import com.opicer.api.goodanswer.infrastructure.GoodAnswerSampleRepository;
import com.opicer.api.mocktest.domain.MockTestComposer;
import com.opicer.api.mocktest.domain.SurveyCatalog;
import com.opicer.api.practice.infrastructure.PracticeSessionRepository;
import com.opicer.api.practice.infrastructure.TopicSelectionRepository;
import com.opicer.api.question.infrastructure.QuestionRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.domain.TopicBadge;
import com.opicer.api.topic.infrastructure.TopicRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicCommandService {

	private final TopicRepository topicRepository;
	private final QuestionRepository questionRepository;
	private final TopicSelectionRepository topicSelectionRepository;
	private final PracticeSessionRepository practiceSessionRepository;
	private final GoodAnswerSampleRepository goodAnswerSampleRepository;

	public TopicCommandService(TopicRepository topicRepository, QuestionRepository questionRepository,
		TopicSelectionRepository topicSelectionRepository, PracticeSessionRepository practiceSessionRepository,
		GoodAnswerSampleRepository goodAnswerSampleRepository) {
		this.topicRepository = topicRepository;
		this.questionRepository = questionRepository;
		this.topicSelectionRepository = topicSelectionRepository;
		this.practiceSessionRepository = practiceSessionRepository;
		this.goodAnswerSampleRepository = goodAnswerSampleRepository;
	}

	@Transactional
	public Topic create(String title, String englishTitle, String category, int categoryOrder, int topicOrder,
		List<TopicBadge> badges, boolean active) {
		String normalized = validated(title, category, null);
		Topic topic = new Topic(normalized, englishTitle, category, categoryOrder, topicOrder, badges, active);
		return topicRepository.save(topic);
	}

	/** A new title carries the topic's questions along, since questions point at their topic by title. */
	@Transactional
	public Optional<Topic> update(UUID id, String title, String englishTitle, String category, int categoryOrder,
		int topicOrder, List<TopicBadge> badges, boolean active) {
		return topicRepository.findById(id)
			.map(existing -> {
				String normalized = validated(title, category, id);
				String previous = existing.getTitle();
				if (!previous.equals(normalized) && isMockLinked(previous)) {
					throw new ApiException(ErrorCode.TOPIC_TITLE_LOCKED,
						"실전연습의 설문 항목이나 첫 문항과 이름으로 연결된 주제라 이름을 바꿀 수 없어요. 이름 외의 항목은 수정할 수 있어요.");
				}
				existing.update(normalized, englishTitle, category, categoryOrder, topicOrder, badges, active);
				if (!previous.equals(normalized)) {
					questionRepository.renameTopic(previous, normalized);
				}
				return topicRepository.save(existing);
			});
	}

	/**
	 * The mock test finds these topics by title in code (survey items in {@link SurveyCatalog} and the
	 * self-introduction), so renaming one would quietly cut it off until that link moves to the admin screen.
	 */
	public static boolean isMockLinked(String title) {
		return MockTestComposer.SELF_INTRO_TOPIC.equals(title) || SurveyCatalog.linkedTopics().contains(title);
	}

	/** Only an unused topic can go; one with questions or practice history is deactivated instead. */
	@Transactional
	public boolean delete(UUID id) {
		Topic topic = topicRepository.findById(id).orElse(null);
		if (topic == null) {
			return false;
		}
		if (questionRepository.existsByTopic(topic.getTitle())
			|| topicSelectionRepository.existsByTopicId(id)
			|| practiceSessionRepository.existsByTopicId(id)
			|| goodAnswerSampleRepository.existsByTopicId(id)) {
			throw new ApiException(ErrorCode.TOPIC_IN_USE,
				"문항이나 연습 기록, 샘플 답변이 있는 주제라 삭제할 수 없어요. 비활성화해 주세요.");
		}
		topicRepository.delete(topic);
		return true;
	}

	private String validated(String title, String category, UUID self) {
		String normalized = title.trim();
		if (!Topic.CATEGORIES.contains(category)) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "분류는 자기소개, 설문조사, 돌발질문 중 하나여야 해요.");
		}
		if (normalized.startsWith(MockTestComposer.ROLE_PLAY_PREFIX)
			|| normalized.startsWith(MockTestComposer.ADVANCED_PREFIX)) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR,
				"'롤플레이:', '고난도:'로 시작하는 이름은 실전 문항 세트용이라 주제 이름으로 쓸 수 없어요.");
		}
		boolean taken = self == null
			? topicRepository.existsByTitle(normalized)
			: topicRepository.existsByTitleAndIdNot(normalized, self);
		if (taken) {
			throw new ApiException(ErrorCode.TOPIC_TITLE_DUPLICATE, "같은 이름의 주제가 이미 있어요.");
		}
		return normalized;
	}
}
