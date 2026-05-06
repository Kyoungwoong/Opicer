package com.opicer.api.practice.application;

import com.opicer.api.credit.application.CreditBalanceService;
import com.opicer.api.practice.domain.PracticeCreditCharge;
import com.opicer.api.practice.domain.TopicSelection;
import com.opicer.api.practice.infrastructure.PracticeCreditChargeRepository;
import com.opicer.api.practice.infrastructure.TopicSelectionRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PracticeSessionService {

	private static final int PRACTICE_COST = 2;

	private final TopicSelectionRepository topicSelectionRepository;
	private final PracticeCreditChargeRepository practiceCreditChargeRepository;
	private final CreditBalanceService creditBalanceService;

	public PracticeSessionService(
		TopicSelectionRepository topicSelectionRepository,
		PracticeCreditChargeRepository practiceCreditChargeRepository,
		CreditBalanceService creditBalanceService
	) {
		this.topicSelectionRepository = topicSelectionRepository;
		this.practiceCreditChargeRepository = practiceCreditChargeRepository;
		this.creditBalanceService = creditBalanceService;
	}

	@Transactional
	public SubmitResult submit(UUID userId, UUID topicSelectionId) {
		TopicSelection selection = topicSelectionRepository.findLockedByIdAndUserId(topicSelectionId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.TOPIC_SELECTION_NOT_FOUND));

		PracticeCreditCharge existing = practiceCreditChargeRepository.findByTopicSelectionId(selection.getId())
			.orElse(null);
		if (existing != null) {
			return new SubmitResult(existing.getId(), true, existing.getAmount());
		}

		boolean deducted = creditBalanceService.deductIfEnough(userId, PRACTICE_COST);
		if (!deducted) {
			throw new ApiException(ErrorCode.CREDIT_INSUFFICIENT_BALANCE, "At least 2 credits are required to submit practice");
		}

		PracticeCreditCharge charge = practiceCreditChargeRepository.save(
			new PracticeCreditCharge(userId, selection.getId(), PRACTICE_COST)
		);
		return new SubmitResult(charge.getId(), false, PRACTICE_COST);
	}

	public record SubmitResult(
		UUID chargeId,
		boolean alreadyCharged,
		int deductedCredits
	) {}
}
