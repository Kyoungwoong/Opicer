package com.opicer.api.mocktest.application;

import com.opicer.api.mocktest.domain.MockTestComposer.Candidate;
import com.opicer.api.mocktest.domain.MockTestComposer.Pool;
import com.opicer.api.question.domain.Question;
import com.opicer.api.question.infrastructure.QuestionRepository;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Reads the active question bank and the practice topics into the composer's pool. */
@Component
public class QuestionPoolLoader {

	static final String CATEGORY_SURVEY = "설문조사";
	static final String CATEGORY_UNEXPECTED = "돌발질문";

	private final QuestionRepository questions;
	private final TopicRepository topics;

	public QuestionPoolLoader(QuestionRepository questions, TopicRepository topics) {
		this.questions = questions;
		this.topics = topics;
	}

	@Transactional(readOnly = true)
	public LoadedPool load() {
		List<Candidate> candidates = new ArrayList<>();
		Map<UUID, QuestionInfo> info = new HashMap<>();
		List<Question> bank = questions.findByActiveTrueOrderByCreatedAtAscIdAsc();
		for (int i = 0; i < bank.size(); i++) {
			Question question = bank.get(i);
			candidates.add(new Candidate(question.getId(), question.getTopic(), question.getType(),
				Set.copyOf(question.getTargetLevels()), i));
			info.put(question.getId(), new QuestionInfo(question.getPromptText(), question.getPromptAudioUrl()));
		}
		Set<String> survey = new LinkedHashSet<>();
		Set<String> unexpected = new LinkedHashSet<>();
		Map<String, UUID> topicIds = new HashMap<>();
		for (Topic topic : topics.findByActiveTrueOrderByCategoryOrderAscTopicOrderAscIdAsc()) {
			topicIds.put(topic.getTitle(), topic.getId());
			if (CATEGORY_SURVEY.equals(topic.getCategory())) {
				survey.add(topic.getTitle());
			} else if (CATEGORY_UNEXPECTED.equals(topic.getCategory())) {
				unexpected.add(topic.getTitle());
			}
		}
		return new LoadedPool(new Pool(List.copyOf(candidates), unexpected, survey), info, topicIds);
	}

	public record QuestionInfo(String promptText, String promptAudioUrl) {}

	public record LoadedPool(Pool pool, Map<UUID, QuestionInfo> questions, Map<String, UUID> topicIds) {

		QuestionInfo info(UUID questionId) {
			return questions.get(questionId);
		}

		/** Null for questions grouped without a practice topic (role-play, advanced). */
		UUID topicId(String topicTitle) {
			return topicIds.get(topicTitle);
		}

		Optional<Candidate> candidate(UUID questionId) {
			return pool.questions().stream().filter(c -> c.id().equals(questionId)).findFirst();
		}
	}
}
