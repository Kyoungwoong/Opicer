package com.opicer.api.topic.infrastructure;

import com.opicer.api.question.domain.Question;
import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.question.infrastructure.QuestionRepository;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.domain.TopicBadge;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PracticeTopicBootstrapRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(PracticeTopicBootstrapRunner.class);

	private static final String CATEGORY_SELF_INTRO = "자기소개";
	private static final String CATEGORY_SURVEY = "설문조사";
	private static final String CATEGORY_UNEXPECTED = "돌발질문";
	private static final String LEGACY_UNEXPECTED = "돌발/상황 대처";
	private static final String SELF_INTRO_TOPIC_TITLE = "자기소개";

	private final TopicRepository topicRepository;
	private final QuestionRepository questionRepository;

	public PracticeTopicBootstrapRunner(
		TopicRepository topicRepository,
		QuestionRepository questionRepository
	) {
		this.topicRepository = topicRepository;
		this.questionRepository = questionRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		reclassifyCategories();
		ensureSelfIntroductionTopicAndQuestions();
	}

	private void reclassifyCategories() {
		List<Topic> topics = topicRepository.findAll();
		int updated = 0;
		for (Topic topic : topics) {
			String newCategory = resolveCategory(topic);
			int newCategoryOrder = resolveCategoryOrder(newCategory);
			if (topic.getCategory().equals(newCategory) && topic.getCategoryOrder() == newCategoryOrder) {
				continue;
			}
			topic.update(
				topic.getTitle(),
				topic.getEnglishTitle(),
				newCategory,
				newCategoryOrder,
				topic.getTopicOrder(),
				topic.getBadges(),
				topic.isActive()
			);
			topicRepository.save(topic);
			updated++;
		}
		if (updated > 0) {
			log.info("Practice category reclassified: {} topics updated", updated);
		}
	}

	private void ensureSelfIntroductionTopicAndQuestions() {
		Optional<Topic> existing = topicRepository.findByTitle(SELF_INTRO_TOPIC_TITLE);
		Topic selfIntro;
		if (existing.isPresent()) {
			Topic topic = existing.get();
			topic.update(
				SELF_INTRO_TOPIC_TITLE,
				"Self Introduction",
				CATEGORY_SELF_INTRO,
				0,
				0,
				List.of(new TopicBadge("필수", null)),
				true
			);
			selfIntro = topicRepository.save(topic);
		} else {
			selfIntro = topicRepository.save(new Topic(
				SELF_INTRO_TOPIC_TITLE,
				"Self Introduction",
				CATEGORY_SELF_INTRO,
				0,
				0,
				List.of(new TopicBadge("필수", null)),
				true
			));
			log.info("Self-introduction topic created: {}", selfIntro.getId());
		}

		List<Question> existingQuestions = questionRepository.findByActiveTrueAndTopicOrderByCreatedAtAscIdAsc(
			SELF_INTRO_TOPIC_TITLE
		);
		if (!existingQuestions.isEmpty()) {
			return;
		}

		List<OpicLevel> baseLevels = List.of(OpicLevel.IL, OpicLevel.IM, OpicLevel.IH);
		questionRepository.saveAll(List.of(
			new Question(
				SELF_INTRO_TOPIC_TITLE,
				QuestionType.DESCRIPTION,
				"Please introduce yourself. Tell me your name, what you do, and what your daily life is like.",
				null,
				"이름/역할 소개 → 평소 일상 → 최근 집중하는 것 순서로 답변하세요.",
				baseLevels,
				List.of("Let me introduce myself", "These days", "In my daily routine")
			),
			new Question(
				SELF_INTRO_TOPIC_TITLE,
				QuestionType.PAST_EXPERIENCE,
				"Tell me about a recent moment that made you proud of yourself. What happened and why was it meaningful?",
				null,
				"상황 설명 → 내가 한 행동 → 결과/느낀 점 순서로 말하세요.",
				baseLevels,
				List.of("Recently", "What I did was", "It was meaningful because")
			),
			new Question(
				SELF_INTRO_TOPIC_TITLE,
				QuestionType.COMPARE_CONTRAST,
				"Compare your current life with your life one or two years ago. What changed the most?",
				null,
				"과거와 현재를 비교하고, 바뀐 이유와 영향을 함께 설명하세요.",
				baseLevels,
				List.of("Compared to", "The biggest change is", "As a result")
			),
			new Question(
				SELF_INTRO_TOPIC_TITLE,
				QuestionType.OPINION,
				"What personal strength helps you most in your daily life, and why do you think so?",
				null,
				"강점 제시 → 실제 사례 → 결론 구조로 답변하세요.",
				baseLevels,
				List.of("I believe my strength is", "For example", "That is why")
			)
		));
		log.info("Self-introduction questions seeded");
	}

	private String resolveCategory(Topic topic) {
		if (SELF_INTRO_TOPIC_TITLE.equals(topic.getTitle())) {
			return CATEGORY_SELF_INTRO;
		}
		if (LEGACY_UNEXPECTED.equals(topic.getCategory()) || CATEGORY_UNEXPECTED.equals(topic.getCategory())) {
			return CATEGORY_UNEXPECTED;
		}
		return CATEGORY_SURVEY;
	}

	private int resolveCategoryOrder(String category) {
		return switch (category) {
			case CATEGORY_SELF_INTRO -> 0;
			case CATEGORY_SURVEY -> 1;
			case CATEGORY_UNEXPECTED -> 2;
			default -> 9;
		};
	}
}
