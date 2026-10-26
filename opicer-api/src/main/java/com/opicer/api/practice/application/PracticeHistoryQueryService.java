package com.opicer.api.practice.application;

import com.opicer.api.practice.domain.PracticeAnswer;
import com.opicer.api.practice.domain.PracticeSession;
import com.opicer.api.practice.infrastructure.PracticeAnswerRepository;
import com.opicer.api.practice.infrastructure.PracticeSessionRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PracticeHistoryQueryService {

	public static final int MAX_LIMIT = 50;

	private final PracticeSessionRepository practiceSessionRepository;
	private final PracticeAnswerRepository practiceAnswerRepository;

	public PracticeHistoryQueryService(
		PracticeSessionRepository practiceSessionRepository,
		PracticeAnswerRepository practiceAnswerRepository
	) {
		this.practiceSessionRepository = practiceSessionRepository;
		this.practiceAnswerRepository = practiceAnswerRepository;
	}

	/** Most recent first. {@code limit} is clamped to 1..{@value #MAX_LIMIT}. */
	public List<SessionSummary> list(UUID userId, int limit) {
		int size = Math.max(1, Math.min(limit, MAX_LIMIT));
		List<PracticeSession> sessions = practiceSessionRepository.findByUserIdOrderByCompletedAtDesc(
			userId, PageRequest.of(0, size)
		);
		if (sessions.isEmpty()) {
			return List.of();
		}

		Map<UUID, List<PracticeAnswer>> answersBySession = practiceAnswerRepository
			.findBySessionIdIn(sessions.stream().map(PracticeSession::getId).toList())
			.stream()
			.collect(Collectors.groupingBy(PracticeAnswer::getSessionId));

		return sessions.stream().map(session -> {
			List<PracticeAnswer> answers = answersBySession.getOrDefault(session.getId(), List.of());
			int analyzed = (int) answers.stream().filter(a -> a.getAnalysis() != null).count();
			return new SessionSummary(session, answers.size(), analyzed);
		}).toList();
	}

	/** Throws 404 for a missing session or one owned by another user. */
	public SessionDetail get(UUID userId, UUID sessionId) {
		PracticeSession session = practiceSessionRepository.findByIdAndUserId(sessionId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.PRACTICE_SESSION_NOT_FOUND));
		return new SessionDetail(session, practiceAnswerRepository.findBySessionIdOrderByQuestionOrderAsc(session.getId()));
	}

	public record SessionSummary(PracticeSession session, int answerCount, int analyzedCount) {}

	public record SessionDetail(PracticeSession session, List<PracticeAnswer> answers) {}
}
