package com.opicer.api.topic.application;

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
public class TopicService {

	private final TopicRepository topicRepository;

	public TopicService(TopicRepository topicRepository) {
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

	@Transactional
	public Topic create(String title, String englishTitle, String category, int categoryOrder, int topicOrder,
		List<TopicBadge> badges, boolean active) {
		Topic topic = new Topic(title, englishTitle, category, categoryOrder, topicOrder, badges, active);
		return topicRepository.save(topic);
	}

	@Transactional
	public Optional<Topic> update(UUID id, String title, String englishTitle, String category, int categoryOrder,
		int topicOrder, List<TopicBadge> badges, boolean active) {
		return topicRepository.findById(id)
			.map(existing -> {
				existing.update(title, englishTitle, category, categoryOrder, topicOrder, badges, active);
				return topicRepository.save(existing);
			});
	}

	@Transactional
	public boolean delete(UUID id) {
		if (!topicRepository.existsById(id)) {
			return false;
		}
		topicRepository.deleteById(id);
		return true;
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
