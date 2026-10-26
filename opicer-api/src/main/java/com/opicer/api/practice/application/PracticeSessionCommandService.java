package com.opicer.api.practice.application;

import com.opicer.api.credit.application.CreditBalanceCommandService;
import com.opicer.api.practice.domain.PracticeAnswer;
import com.opicer.api.practice.domain.PracticeCreditCharge;
import com.opicer.api.practice.domain.PracticeSession;
import com.opicer.api.practice.domain.TopicSelection;
import com.opicer.api.practice.infrastructure.PracticeAnswerRepository;
import com.opicer.api.practice.infrastructure.PracticeCreditChargeRepository;
import com.opicer.api.practice.infrastructure.PracticeSessionRepository;
import com.opicer.api.practice.infrastructure.TopicSelectionRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PracticeSessionCommandService {

	private static final int PRACTICE_COST = 2;
	private static final String FALLBACK_TOPIC_TITLE = "주제별 연습";

	private final TopicSelectionRepository topicSelectionRepository;
	private final PracticeCreditChargeRepository practiceCreditChargeRepository;
	private final CreditBalanceCommandService creditBalanceCommandService;
	private final PracticeSessionRepository practiceSessionRepository;
	private final PracticeAnswerRepository practiceAnswerRepository;
	private final TopicRepository topicRepository;

	public PracticeSessionCommandService(
		TopicSelectionRepository topicSelectionRepository,
		PracticeCreditChargeRepository practiceCreditChargeRepository,
		CreditBalanceCommandService creditBalanceCommandService,
		PracticeSessionRepository practiceSessionRepository,
		PracticeAnswerRepository practiceAnswerRepository,
		TopicRepository topicRepository
	) {
		this.topicSelectionRepository = topicSelectionRepository;
		this.practiceCreditChargeRepository = practiceCreditChargeRepository;
		this.creditBalanceCommandService = creditBalanceCommandService;
		this.practiceSessionRepository = practiceSessionRepository;
		this.practiceAnswerRepository = practiceAnswerRepository;
		this.topicRepository = topicRepository;
	}

	@Transactional
	public SubmitResult submit(UUID userId, UUID topicSelectionId) {
		return submit(userId, topicSelectionId, List.of());
	}

	/**
	 * Charges the practice once per topic selection and records the session with its answers.
	 * Re-submitting the same selection is idempotent: no second charge and no second session.
	 */
	@Transactional
	public SubmitResult submit(UUID userId, UUID topicSelectionId, List<AnswerInput> answers) {
		TopicSelection selection = topicSelectionRepository.findLockedByIdAndUserId(topicSelectionId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.TOPIC_SELECTION_NOT_FOUND));

		PracticeCreditCharge existing = practiceCreditChargeRepository.findByTopicSelectionId(selection.getId())
			.orElse(null);
		if (existing != null) {
			PracticeSession session = recordSession(selection, answers);
			return new SubmitResult(existing.getId(), true, existing.getAmount(), session.getId());
		}

		boolean deducted = creditBalanceCommandService.deductIfEnough(userId, PRACTICE_COST);
		if (!deducted) {
			throw new ApiException(ErrorCode.CREDIT_INSUFFICIENT_BALANCE, "At least 2 credits are required to submit practice");
		}

		PracticeCreditCharge charge = practiceCreditChargeRepository.save(
			new PracticeCreditCharge(userId, selection.getId(), PRACTICE_COST)
		);
		PracticeSession session = recordSession(selection, answers);
		return new SubmitResult(charge.getId(), false, PRACTICE_COST, session.getId());
	}

	/** Runs under the selection's row lock, so at most one session is created per selection. */
	private PracticeSession recordSession(TopicSelection selection, List<AnswerInput> answers) {
		PracticeSession existing = practiceSessionRepository.findByTopicSelectionId(selection.getId()).orElse(null);
		if (existing != null) {
			return existing;
		}

		String topicTitle = topicRepository.findById(selection.getTopicId())
			.map(Topic::getTitle)
			.orElse(FALLBACK_TOPIC_TITLE);
		PracticeSession session = practiceSessionRepository.save(new PracticeSession(
			selection.getUserId(),
			selection.getId(),
			selection.getTopicId(),
			topicTitle,
			selection.getSelectedAt()
		));

		List<PracticeAnswer> rows = new ArrayList<>();
		for (int i = 0; i < answers.size(); i++) {
			AnswerInput answer = answers.get(i);
			rows.add(new PracticeAnswer(session.getId(), answer.questionId(), i, answer.questionText(), answer.transcript()));
		}
		practiceAnswerRepository.saveAll(rows);
		return session;
	}

	public record AnswerInput(UUID questionId, String questionText, String transcript) {}

	public record SubmitResult(
		UUID chargeId,
		boolean alreadyCharged,
		int deductedCredits,
		UUID sessionId
	) {}
}
