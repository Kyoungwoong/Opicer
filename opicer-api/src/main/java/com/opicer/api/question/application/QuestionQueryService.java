package com.opicer.api.question.application;

import com.opicer.api.question.domain.Question;
import com.opicer.api.question.infrastructure.QuestionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionQueryService {

	private final QuestionRepository questionRepository;

	public QuestionQueryService(QuestionRepository questionRepository) {
		this.questionRepository = questionRepository;
	}

	@Transactional(readOnly = true)
	public List<Question> findAll() {
		return questionRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<Question> findActiveByTopic(String topic) {
		return questionRepository.findByActiveTrueAndTopicOrderByCreatedAtAscIdAsc(topic);
	}

	@Transactional(readOnly = true)
	public Optional<Question> findById(UUID id) {
		return questionRepository.findById(id);
	}
}
