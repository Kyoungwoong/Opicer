package com.opicer.api.practice.application;

import com.opicer.api.credit.application.CreditBalanceQueryService;
import com.opicer.api.practice.domain.TopicSelection;
import com.opicer.api.practice.infrastructure.TopicSelectionRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.topic.application.TopicQueryService;
import com.opicer.api.topic.domain.Topic;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicSelectionCommandService {

	private final TopicSelectionRepository topicSelectionRepository;
	private final TopicQueryService topicQueryService;
	private final CreditBalanceQueryService creditBalanceQueryService;

	public TopicSelectionCommandService(
		TopicSelectionRepository topicSelectionRepository,
		TopicQueryService topicQueryService,
		CreditBalanceQueryService creditBalanceQueryService
	) {
		this.topicSelectionRepository = topicSelectionRepository;
		this.topicQueryService = topicQueryService;
		this.creditBalanceQueryService = creditBalanceQueryService;
	}

	@Transactional
	public TopicSelection createSelection(UUID userId, UUID topicId) {
		if (!creditBalanceQueryService.hasEnough(userId, 2)) {
			throw new ApiException(ErrorCode.CREDIT_INSUFFICIENT_BALANCE, "At least 2 credits are required to start practice");
		}
		Topic topic = topicQueryService.getActiveOrThrow(topicId);
		TopicSelection selection = new TopicSelection(userId, topic.getId());
		return topicSelectionRepository.save(selection);
	}
}
