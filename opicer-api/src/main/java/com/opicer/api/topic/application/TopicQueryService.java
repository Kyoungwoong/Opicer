package com.opicer.api.topic.application;

import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicQueryService {

	private final TopicRepository topicRepository;

	public TopicQueryService(TopicRepository topicRepository) {
		this.topicRepository = topicRepository;
	}

	@Transactional(readOnly = true)
	public List<Topic> findAll() {
		return topicRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<Topic> findActive() {
		return topicRepository.findByActiveTrueOrderByCategoryOrderAscTopicOrderAscIdAsc();
	}

	@Transactional(readOnly = true)
	public Topic getActiveOrThrow(UUID id) {
		Topic topic = topicRepository.findById(id)
			.orElseThrow(() -> new ApiException(ErrorCode.TOPIC_NOT_FOUND, "Topic not found: " + id));
		if (!topic.isActive()) {
			throw new ApiException(ErrorCode.TOPIC_INACTIVE, "Topic is inactive: " + id);
		}
		return topic;
	}
}
