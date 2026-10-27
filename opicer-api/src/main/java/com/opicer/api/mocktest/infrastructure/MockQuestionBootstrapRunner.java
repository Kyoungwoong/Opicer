package com.opicer.api.mocktest.infrastructure;

import com.opicer.api.mocktest.domain.MockTestComposer;
import com.opicer.api.question.domain.Question;
import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.question.infrastructure.QuestionRepository;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds the role-play and advanced questions the mock test needs and the bank did not have. Written for Opicer (not
 * copied from any exam). Inserted only when missing, so admins can edit or deactivate them in the question tab.
 * They have no practice topic row, so the topic practice list does not show them.
 */
@Component
public class MockQuestionBootstrapRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MockQuestionBootstrapRunner.class);

	private static final List<OpicLevel> ROLE_PLAY_LEVELS = List.of(OpicLevel.IM, OpicLevel.IH, OpicLevel.AL);
	private static final List<OpicLevel> ADVANCED_LEVELS = List.of(OpicLevel.IH, OpicLevel.AL);
	private static final String ADVANCED_TOPIC = MockTestComposer.ADVANCED_PREFIX + " 사회·변화";

	/** Each set: ask questions → solve a problem → a related experience. */
	static final List<Seed> ROLE_PLAY = List.of(
		roleplay("호텔 예약",
			"You are planning a trip and want to stay at a hotel. Call the hotel and ask three or four questions about the room and the services they offer.",
			"When you arrive at the hotel, you find out that your reservation was cancelled by mistake and there are no rooms left. Call a friend, explain the situation, and suggest two or three alternatives.",
			"Have you ever had a problem with a reservation or a booking? Tell me what happened, how you dealt with it, and how it ended."),
		roleplay("공연 티켓",
			"You want to go to a concert with a friend this weekend. Call the box office and ask three or four questions to get the information you need.",
			"On the day of the concert, you realise the tickets you bought are for the wrong date. Call the box office, explain the problem, and offer two or three solutions.",
			"Tell me about a time when your plans for a show, a game or another event did not go as expected. What happened and what did you do?"),
		roleplay("가구 배송",
			"You ordered a new piece of furniture online. Call the store and ask three or four questions about the delivery.",
			"The furniture arrived, but it is damaged and one part is missing. Call the store, describe the problem, and suggest two or three ways to solve it.",
			"Have you ever bought something that turned out to be broken or different from what you expected? Describe what happened and how the problem was solved."),
		roleplay("헬스장 등록",
			"You want to join a gym in your neighborhood. Call the gym and ask three or four questions about memberships and classes.",
			"After joining, you have to move to another city for work and can no longer use the gym. Call the gym, explain the situation, and suggest two or three options.",
			"Tell me about a time when you had to cancel or change a plan because of an unexpected situation. What did you do?")
	);

	static final List<Seed> ADVANCED = List.of(
		advanced(QuestionType.COMPARE_CONTRAST,
			"Compare how people in your country spent their free time ten years ago with how they spend it today. What has changed and why?"),
		advanced(QuestionType.OPINION,
			"Many people now shop online instead of going to stores. What are the advantages and disadvantages of this trend? Give your opinion."),
		advanced(QuestionType.COMPARE_CONTRAST,
			"How has technology changed the way people communicate with their friends and family? Compare the past with the present."),
		advanced(QuestionType.OPINION,
			"Some people say that working from home is better than working in an office. What do you think? Explain your reasons with examples."),
		advanced(QuestionType.COMPARE_CONTRAST,
			"Think about the place where you live. How is it different from how it was when you were a child, and how do you feel about those changes?"),
		advanced(QuestionType.OPINION,
			"What are some concerns people have about the environment these days, and what do you think individuals can do about them?"),
		advanced(QuestionType.COMPARE_CONTRAST,
			"Compare the way young people and older people in your country travel. What are the main differences?"),
		advanced(QuestionType.OPINION,
			"Many people say that social media has changed how we spend our time. Do you think this change is positive or negative? Why?")
	);

	private final QuestionRepository questionRepository;

	public MockQuestionBootstrapRunner(QuestionRepository questionRepository) {
		this.questionRepository = questionRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		int added = 0;
		for (Seed seed : ROLE_PLAY) {
			added += insertIfMissing(seed);
		}
		for (Seed seed : ADVANCED) {
			added += insertIfMissing(seed);
		}
		if (added > 0) {
			log.info("[MockTest] seeded {} role-play/advanced questions", added);
		}
	}

	/** Role-play sets are asked in insertion order, so the three questions of a set are saved one by one. */
	private int insertIfMissing(Seed seed) {
		int added = 0;
		for (int i = 0; i < seed.prompts().size(); i++) {
			String prompt = seed.prompts().get(i);
			if (questionRepository.existsByTopicAndPromptText(seed.topic(), prompt)) {
				continue;
			}
			questionRepository.saveAndFlush(new Question(seed.topic(), seed.types().get(i), prompt, null, null,
				seed.levels(), List.of()));
			added++;
		}
		return added;
	}

	private static Seed roleplay(String scenario, String ask, String solve, String experience) {
		return new Seed(MockTestComposer.ROLE_PLAY_PREFIX + " " + scenario,
			List.of(QuestionType.ROLE_PLAY, QuestionType.ROLE_PLAY, QuestionType.PAST_EXPERIENCE),
			List.of(ask, solve, experience), ROLE_PLAY_LEVELS);
	}

	private static Seed advanced(QuestionType type, String prompt) {
		return new Seed(ADVANCED_TOPIC, List.of(type), List.of(prompt), ADVANCED_LEVELS);
	}

	record Seed(String topic, List<QuestionType> types, List<String> prompts, List<OpicLevel> levels) {}
}
