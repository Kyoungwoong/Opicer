package com.opicer.api.practice.application;

import com.opicer.api.aijob.application.AiJobCommandService;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.config.WorkerProperties;
import com.opicer.api.practice.domain.AnswerEvaluationStatus;
import com.opicer.api.practice.domain.PracticeAnswer;
import com.opicer.api.practice.domain.PracticeSession;
import com.opicer.api.practice.domain.SessionEvaluationStatus;
import com.opicer.api.practice.infrastructure.PracticeAnswerRepository;
import com.opicer.api.practice.infrastructure.PracticeSessionRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PracticeEvaluationCommandService {

	private static final Set<SessionEvaluationStatus> RETRYABLE =
		Set.of(SessionEvaluationStatus.FAILED, SessionEvaluationStatus.PARTIAL);

	private final PracticeSessionRepository sessionRepository;
	private final PracticeAnswerRepository answerRepository;
	private final AiJobCommandService aiJobCommandService;
	private final WorkerProperties workerProperties;

	public PracticeEvaluationCommandService(PracticeSessionRepository sessionRepository,
		PracticeAnswerRepository answerRepository, AiJobCommandService aiJobCommandService,
		WorkerProperties workerProperties) {
		this.sessionRepository = sessionRepository;
		this.answerRepository = answerRepository;
		this.aiJobCommandService = aiJobCommandService;
		this.workerProperties = workerProperties;
	}

	/**
	 * Learner-requested retry of a failed or partial evaluation. Free; limited per session. Only the answers that
	 * failed are evaluated again — finished and skipped ones keep their results.
	 */
	@Transactional
	public SessionEvaluationStatus retry(UUID userId, UUID sessionId) {
		PracticeSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.PRACTICE_SESSION_NOT_FOUND));
		if (!RETRYABLE.contains(session.getEvaluationStatus())) {
			throw new ApiException(ErrorCode.EVALUATION_NOT_RETRYABLE);
		}
		if (!aiJobCommandService.requeueManually(AiJobType.SESSION_EVALUATION, session.getId(),
			workerProperties.getMaxManualRetries())) {
			throw new ApiException(ErrorCode.EVALUATION_RETRY_LIMIT);
		}
		for (PracticeAnswer answer : answerRepository.findBySessionIdOrderByQuestionOrderAsc(session.getId())) {
			if (answer.getEvaluationStatus() == AnswerEvaluationStatus.FAILED) {
				answer.markEvaluationPending();
			}
		}
		session.requestEvaluation(session.getTargetLevel());
		return session.getEvaluationStatus();
	}
}
