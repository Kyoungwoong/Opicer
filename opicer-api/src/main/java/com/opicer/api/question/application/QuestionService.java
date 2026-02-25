package com.opicer.api.question.application;

import com.opicer.api.question.domain.Question;
import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.question.infrastructure.QuestionRepository;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {

	private final QuestionRepository questionRepository;

	public QuestionService(QuestionRepository questionRepository) {
		this.questionRepository = questionRepository;
	}

	@Transactional(readOnly = true)
	public List<Question> findAll() {
		return questionRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Optional<Question> findById(UUID id) {
		return questionRepository.findById(id);
	}

	@Transactional
	public Question create(String topic, QuestionType type, String promptText, String promptAudioUrl,
		String structuralHint, List<OpicLevel> targetLevels, List<String> keyExpressions) {
		Question question = new Question(topic, type, promptText, promptAudioUrl,
			structuralHint, targetLevels, keyExpressions);
		return questionRepository.save(question);
	}

	@Transactional
	public Optional<Question> update(UUID id, String topic, QuestionType type, String promptText,
		String promptAudioUrl, String structuralHint, List<OpicLevel> targetLevels,
		List<String> keyExpressions, boolean active) {
		return questionRepository.findById(id).map(question -> {
			question.update(topic, type, promptText, promptAudioUrl, structuralHint,
				targetLevels, keyExpressions, active);
			return questionRepository.save(question);
		});
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
