package com.opicer.api.practice.application;

import com.opicer.api.aijob.application.AiJobCommandService;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.credit.application.CreditBalanceCommandService;
import com.opicer.api.practice.domain.PracticeAnswer;
import com.opicer.api.practice.domain.PracticeCreditCharge;
import com.opicer.api.practice.domain.PracticeSession;
import com.opicer.api.practice.domain.SessionEvaluationStatus;
import com.opicer.api.practice.domain.TopicSelection;
import com.opicer.api.practice.infrastructure.PracticeAnswerRepository;
import com.opicer.api.practice.infrastructure.PracticeCreditChargeRepository;
import com.opicer.api.practice.infrastructure.PracticeSessionRepository;
import com.opicer.api.practice.infrastructure.TopicSelectionRepository;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PracticeSessionCommandService {

	private static final int PRACTICE_COST = 2;
	private static final String FALLBACK_TOPIC_TITLE = "주제별 연습";
	public static final OpicLevel DEFAULT_TARGET_LEVEL = OpicLevel.IM;
	/** Goals a learner can practise toward; NL/IL are not offered as targets. */
	public static final Set<OpicLevel> TARGET_LEVELS = Set.of(OpicLevel.IM, OpicLevel.IH, OpicLevel.AL);

	private final TopicSelectionRepository topicSelectionRepository;
	private final PracticeCreditChargeRepository practiceCreditChargeRepository;
	private final CreditBalanceCommandService creditBalanceCommandService;
	private final PracticeSessionRepository practiceSessionRepository;
	private final PracticeAnswerRepository practiceAnswerRepository;
	private final TopicRepository topicRepository;
	private final AiJobCommandService aiJobCommandService;

	public PracticeSessionCommandService(
		TopicSelectionRepository topicSelectionRepository,
		PracticeCreditChargeRepository practiceCreditChargeRepository,
		CreditBalanceCommandService creditBalanceCommandService,
		PracticeSessionRepository practiceSessionRepository,
		PracticeAnswerRepository practiceAnswerRepository,
		TopicRepository topicRepository,
		AiJobCommandService aiJobCommandService
	) {
		this.topicSelectionRepository = topicSelectionRepository;
		this.practiceCreditChargeRepository = practiceCreditChargeRepository;
		this.creditBalanceCommandService = creditBalanceCommandService;
		this.practiceSessionRepository = practiceSessionRepository;
		this.practiceAnswerRepository = practiceAnswerRepository;
		this.topicRepository = topicRepository;
		this.aiJobCommandService = aiJobCommandService;
	}

	@Transactional
	public SubmitResult submit(UUID userId, UUID topicSelectionId) {
		return submit(userId, topicSelectionId, List.of());
	}

	@Transactional
	public SubmitResult submit(UUID userId, UUID topicSelectionId, List<AnswerInput> answers) {
		return submit(userId, topicSelectionId, null, answers);
	}

	/**
	 * Charges the practice once per topic selection, records the session with its answers, and queues its
	 * automatic evaluation against {@code targetLevel} (null = {@link #DEFAULT_TARGET_LEVEL}).
	 * Re-submitting the same selection is idempotent: no second charge, session or evaluation job.
	 */
	@Transactional
	public SubmitResult submit(UUID userId, UUID topicSelectionId, OpicLevel targetLevel, List<AnswerInput> answers) {
		OpicLevel target = resolveTargetLevel(targetLevel);
		TopicSelection selection = topicSelectionRepository.findLockedByIdAndUserId(topicSelectionId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.TOPIC_SELECTION_NOT_FOUND));

		PracticeCreditCharge existing = practiceCreditChargeRepository.findByTopicSelectionId(selection.getId())
			.orElse(null);
		if (existing != null) {
			PracticeSession session = recordSession(selection, target, answers);
			return new SubmitResult(existing.getId(), true, existing.getAmount(), session.getId(), session.getEvaluationStatus());
		}

		boolean deducted = creditBalanceCommandService.deductIfEnough(userId, PRACTICE_COST);
		if (!deducted) {
			throw new ApiException(ErrorCode.CREDIT_INSUFFICIENT_BALANCE, "At least 2 credits are required to submit practice");
		}

		PracticeCreditCharge charge = practiceCreditChargeRepository.save(
			new PracticeCreditCharge(userId, selection.getId(), PRACTICE_COST)
		);
		PracticeSession session = recordSession(selection, target, answers);
		return new SubmitResult(charge.getId(), false, PRACTICE_COST, session.getId(), session.getEvaluationStatus());
	}

	/** Runs under the selection's row lock, so at most one session is created per selection. */
	private PracticeSession recordSession(TopicSelection selection, OpicLevel targetLevel, List<AnswerInput> answers) {
		PracticeSession existing = practiceSessionRepository.findByTopicSelectionId(selection.getId()).orElse(null);
		if (existing != null) {
			return existing;
		}

		String topicTitle = topicRepository.findById(selection.getTopicId())
			.map(Topic::getTitle)
			.orElse(FALLBACK_TOPIC_TITLE);
		PracticeSession session = new PracticeSession(
			selection.getUserId(),
			selection.getId(),
			selection.getTopicId(),
			topicTitle,
			selection.getSelectedAt()
		);
		boolean evaluate = !answers.isEmpty();
		if (evaluate) {
			session.requestEvaluation(targetLevel);
		}
		session = practiceSessionRepository.save(session);

		List<PracticeAnswer> rows = new ArrayList<>();
		for (int i = 0; i < answers.size(); i++) {
			AnswerInput answer = answers.get(i);
			PracticeAnswer row = new PracticeAnswer(session.getId(), answer.questionId(), i, answer.questionText(),
				answer.transcript(), answer.durationSeconds());
			row.markEvaluationPending();
			rows.add(row);
		}
		practiceAnswerRepository.saveAll(rows);
		if (evaluate) {
			// Same transaction: the job only exists if the session and answers commit.
			aiJobCommandService.enqueue(AiJobType.SESSION_EVALUATION, session.getId());
		}
		return session;
	}

	static OpicLevel resolveTargetLevel(OpicLevel requested) {
		if (requested == null) {
			return DEFAULT_TARGET_LEVEL;
		}
		if (!TARGET_LEVELS.contains(requested)) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "targetLevel must be one of IM, IH, AL");
		}
		return requested;
	}

	/** @param durationSeconds recording length measured by the client; null when unknown */
	public record AnswerInput(UUID questionId, String questionText, String transcript, Integer durationSeconds) {
		public AnswerInput(UUID questionId, String questionText, String transcript) {
			this(questionId, questionText, transcript, null);
		}
	}

	/** @param evaluationStatus null when there was nothing to evaluate */
	public record SubmitResult(
		UUID chargeId,
		boolean alreadyCharged,
		int deductedCredits,
		UUID sessionId,
		SessionEvaluationStatus evaluationStatus
	) {}
}
