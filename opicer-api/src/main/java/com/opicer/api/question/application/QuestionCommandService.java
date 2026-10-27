package com.opicer.api.question.application;

import com.opicer.api.mocktest.domain.MockTestComposer;
import com.opicer.api.question.domain.Question;
import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.question.infrastructure.QuestionRepository;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.topic.infrastructure.TopicRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionCommandService {

	private final QuestionRepository questionRepository;
	private final TopicRepository topicRepository;

	public QuestionCommandService(QuestionRepository questionRepository, TopicRepository topicRepository) {
		this.questionRepository = questionRepository;
		this.topicRepository = topicRepository;
	}

	@Transactional
	public Question create(String topic, QuestionType type, String promptText, String promptAudioUrl,
		String structuralHint, List<OpicLevel> targetLevels, List<String> keyExpressions) {
		Question question = new Question(linkedTopic(topic), type, promptText, promptAudioUrl,
			structuralHint, targetLevels, keyExpressions);
		return questionRepository.save(question);
	}

	@Transactional
	public Optional<Question> update(UUID id, String topic, QuestionType type, String promptText,
		String promptAudioUrl, String structuralHint, List<OpicLevel> targetLevels,
		List<String> keyExpressions, boolean active) {
		return questionRepository.findById(id).map(question -> {
			question.update(linkedTopic(topic), type, promptText, promptAudioUrl, structuralHint,
				targetLevels, keyExpressions, active);
			return questionRepository.save(question);
		});
	}

	/**
	 * A question belongs to a registered topic (by title) or to a mock-test set named "롤플레이: …" or "고난도: …";
	 * anything else would sit in the bank where no screen ever picks it.
	 */
	private String linkedTopic(String topic) {
		String normalized = topic.trim();
		for (String prefix : List.of(MockTestComposer.ROLE_PLAY_PREFIX, MockTestComposer.ADVANCED_PREFIX)) {
			if (normalized.startsWith(prefix)) {
				if (normalized.substring(prefix.length()).isBlank()) {
					throw new ApiException(ErrorCode.VALIDATION_ERROR, "'" + prefix + "' 뒤에 세트 이름을 적어 주세요.");
				}
				return normalized;
			}
		}
		if (!topicRepository.existsByTitle(normalized)) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR,
				"등록된 주제가 아니에요. 주제 관리 탭에서 먼저 주제를 만들거나 롤플레이·고난도 세트 이름을 써 주세요.");
		}
		return normalized;
	}

	@Transactional
	public boolean delete(UUID id) {
		if (!questionRepository.existsById(id)) {
			return false;
		}
		questionRepository.deleteById(id);
		return true;
	}
}
