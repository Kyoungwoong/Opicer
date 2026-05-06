package com.opicer.api.practice.application;

import com.opicer.api.credit.application.CreditBalanceService;
import com.opicer.api.practice.domain.TopicSelection;
import com.opicer.api.practice.infrastructure.TopicSelectionRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.topic.application.TopicService;
import com.opicer.api.topic.domain.Topic;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicSelectionService {

	private final TopicSelectionRepository topicSelectionRepository;
	private final TopicService topicService;
	private final CreditBalanceService creditBalanceService;

	public TopicSelectionService(
		TopicSelectionRepository topicSelectionRepository,
		TopicService topicService,
		CreditBalanceService creditBalanceService
	) {
		this.topicSelectionRepository = topicSelectionRepository;
		this.topicService = topicService;
		this.creditBalanceService = creditBalanceService;
	}

	@Transactional
	public TopicSelection createSelection(UUID userId, UUID topicId) {
		if (!creditBalanceService.hasEnough(userId, 2)) {
			throw new ApiException(ErrorCode.CREDIT_INSUFFICIENT_BALANCE, "At least 2 credits are required to start practice");
		}
		Topic topic = topicService.getActiveOrThrow(topicId);
		TopicSelection selection = new TopicSelection(userId, topic.getId());
		return topicSelectionRepository.save(selection);
	}
}
