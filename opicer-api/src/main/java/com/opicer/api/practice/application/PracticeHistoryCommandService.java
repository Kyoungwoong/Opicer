package com.opicer.api.practice.application;

import com.opicer.api.aijob.application.AiJobCommandService;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.practice.domain.PracticeSession;
import com.opicer.api.practice.infrastructure.PracticeAnswerRepository;
import com.opicer.api.practice.infrastructure.PracticeSessionRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PracticeHistoryCommandService {

	private final PracticeSessionRepository practiceSessionRepository;
	private final PracticeAnswerRepository practiceAnswerRepository;
	private final AiJobCommandService aiJobCommandService;

	public PracticeHistoryCommandService(
		PracticeSessionRepository practiceSessionRepository,
		PracticeAnswerRepository practiceAnswerRepository,
		AiJobCommandService aiJobCommandService
	) {
		this.practiceSessionRepository = practiceSessionRepository;
		this.practiceAnswerRepository = practiceAnswerRepository;
		this.aiJobCommandService = aiJobCommandService;
	}

	/** Physically deletes the session, every answer derived from it, and its evaluation job. */
	@Transactional
	public void delete(UUID userId, UUID sessionId) {
		PracticeSession session = findOwned(userId, sessionId);
		aiJobCommandService.delete(AiJobType.SESSION_EVALUATION, session.getId());
		practiceAnswerRepository.deleteBySessionId(session.getId());
		practiceSessionRepository.delete(session);
	}

	private PracticeSession findOwned(UUID userId, UUID sessionId) {
		return practiceSessionRepository.findByIdAndUserId(sessionId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.PRACTICE_SESSION_NOT_FOUND));
	}
}
