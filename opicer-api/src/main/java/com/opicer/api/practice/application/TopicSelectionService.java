package com.opicer.api.practice.application;

import com.opicer.api.practice.domain.TopicSelection;
import com.opicer.api.practice.infrastructure.TopicSelectionRepository;
import com.opicer.api.topic.application.TopicService;
import com.opicer.api.topic.domain.Topic;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicSelectionService {

	private final TopicSelectionRepository topicSelectionRepository;
	private final TopicService topicService;

	public TopicSelectionService(TopicSelectionRepository topicSelectionRepository, TopicService topicService) {
		this.topicSelectionRepository = topicSelectionRepository;
		this.topicService = topicService;
	}

	@Transactional
	public TopicSelection createSelection(UUID userId, UUID topicId) {
		Topic topic = topicService.getActiveOrThrow(topicId);
		TopicSelection selection = new TopicSelection(userId, topic.getId());
		return topicSelectionRepository.save(selection);
	}
}
