package com.opicer.api.practice.application;

import com.opicer.api.practice.domain.PracticeAnswer;
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

	public PracticeHistoryCommandService(
		PracticeSessionRepository practiceSessionRepository,
		PracticeAnswerRepository practiceAnswerRepository
	) {
		this.practiceSessionRepository = practiceSessionRepository;
		this.practiceAnswerRepository = practiceAnswerRepository;
	}

	/** Stores AI analysis and/or the improved script for one answer; null fields are left as they are. */
	@Transactional
	public PracticeAnswer recordAiResult(UUID userId, UUID sessionId, UUID questionId, String analysis, String improvedScript) {
		PracticeSession session = findOwned(userId, sessionId);
		PracticeAnswer answer = practiceAnswerRepository.findBySessionIdAndQuestionId(session.getId(), questionId)
			.orElseThrow(() -> new ApiException(ErrorCode.PRACTICE_ANSWER_NOT_FOUND));
		answer.recordAiResult(analysis, improvedScript);
		return answer;
	}

	/** Physically deletes the session and every answer derived from it. */
	@Transactional
	public void delete(UUID userId, UUID sessionId) {
		PracticeSession session = findOwned(userId, sessionId);
		practiceAnswerRepository.deleteBySessionId(session.getId());
		practiceSessionRepository.delete(session);
	}

	private PracticeSession findOwned(UUID userId, UUID sessionId) {
		return practiceSessionRepository.findByIdAndUserId(sessionId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.PRACTICE_SESSION_NOT_FOUND));
	}
}
