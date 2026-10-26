package com.opicer.api.practice.application;

import com.opicer.api.credit.application.CreditBalanceCommandService;
import com.opicer.api.practice.domain.PracticeAnswer;
import com.opicer.api.practice.domain.TopicSelection;
import com.opicer.api.practice.infrastructure.PracticeAnswerRepository;
import com.opicer.api.practice.infrastructure.PracticeCreditChargeRepository;
import com.opicer.api.practice.infrastructure.PracticeSessionRepository;
import com.opicer.api.practice.infrastructure.TopicSelectionRepository;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.User;
import com.opicer.api.user.domain.UserRole;
import com.opicer.api.user.infrastructure.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PracticeHistoryServiceTest {

	@Autowired
	private PracticeSessionCommandService practiceSessionCommandService;

	@Autowired
	private PracticeHistoryQueryService practiceHistoryQueryService;

	@Autowired
	private PracticeHistoryCommandService practiceHistoryCommandService;

	@Autowired
	private TopicSelectionRepository topicSelectionRepository;

	@Autowired
	private PracticeCreditChargeRepository practiceCreditChargeRepository;

	@Autowired
	private PracticeSessionRepository practiceSessionRepository;

	@Autowired
	private PracticeAnswerRepository practiceAnswerRepository;

	@Autowired
	private CreditBalanceCommandService creditBalanceService;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		practiceAnswerRepository.deleteAll();
		practiceSessionRepository.deleteAll();
		practiceCreditChargeRepository.deleteAll();
		topicSelectionRepository.deleteAll();
	}

	@Test
	void submitRecordsSessionWithAnswersInOrder() {
		UUID userId = userWithCredits(10);
		TopicSelection selection = topicSelectionRepository.save(new TopicSelection(userId, UUID.randomUUID()));
		UUID q1 = UUID.randomUUID();
		UUID q2 = UUID.randomUUID();

		PracticeSessionCommandService.SubmitResult result = practiceSessionCommandService.submit(userId, selection.getId(), List.of(
			new PracticeSessionCommandService.AnswerInput(q1, "Describe your home.", "I live in an apartment."),
			new PracticeSessionCommandService.AnswerInput(q2, "Tell me about a trip.", null)
		));

		PracticeHistoryQueryService.SessionDetail detail = practiceHistoryQueryService.get(userId, result.sessionId());
		assertThat(detail.session().getTopicSelectionId()).isEqualTo(selection.getId());
		assertThat(detail.session().getTopicTitle()).isEqualTo("주제별 연습");
		assertThat(detail.answers()).extracting(a -> a.getQuestionId()).containsExactly(q1, q2);
		assertThat(detail.answers().get(0).getTranscript()).isEqualTo("I live in an apartment.");
		assertThat(detail.answers().get(1).getTranscript()).isNull();
	}

	@Test
	void resubmittingSameSelectionKeepsOneSession() {
		UUID userId = userWithCredits(10);
		TopicSelection selection = topicSelectionRepository.save(new TopicSelection(userId, UUID.randomUUID()));
		List<PracticeSessionCommandService.AnswerInput> answers = List.of(
			new PracticeSessionCommandService.AnswerInput(UUID.randomUUID(), "Q", "A")
		);

		PracticeSessionCommandService.SubmitResult first = practiceSessionCommandService.submit(userId, selection.getId(), answers);
		PracticeSessionCommandService.SubmitResult second = practiceSessionCommandService.submit(userId, selection.getId(), answers);

		assertThat(second.alreadyCharged()).isTrue();
		assertThat(second.sessionId()).isEqualTo(first.sessionId());
		assertThat(practiceSessionRepository.count()).isEqualTo(1);
		assertThat(practiceAnswerRepository.count()).isEqualTo(1);
	}

	@Test
	void listReturnsOnlyOwnSessionsNewestFirstWithCounts() throws InterruptedException {
		UUID owner = userWithCredits(10);
		UUID other = userWithCredits(10);
		UUID older = submit(owner, 2).sessionId();
		Thread.sleep(20); // distinct completedAt for a deterministic order
		UUID newer = submit(owner, 1).sessionId();
		submit(other, 1);

		PracticeAnswer evaluated = practiceHistoryQueryService.get(owner, older).answers().get(0);
		evaluated.recordEvaluation("{}", OpicLevel.IM, "analysis", "script");
		practiceAnswerRepository.save(evaluated);

		List<PracticeHistoryQueryService.SessionSummary> list = practiceHistoryQueryService.list(owner, 10);
		assertThat(list).extracting(s -> s.session().getId()).containsExactly(newer, older);
		assertThat(list.get(1).answerCount()).isEqualTo(2);
		assertThat(list.get(1).analyzedCount()).isEqualTo(1);
		assertThat(practiceHistoryQueryService.list(owner, 1)).hasSize(1);
	}

	@Test
	void otherUsersSessionIsNotFound() {
		UUID owner = userWithCredits(10);
		UUID attacker = userWithCredits(10);
		UUID sessionId = submit(owner, 1).sessionId();

		assertThatThrownBy(() -> practiceHistoryQueryService.get(attacker, sessionId))
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PRACTICE_SESSION_NOT_FOUND));
		assertThatThrownBy(() -> practiceHistoryCommandService.delete(attacker, sessionId))
			.isInstanceOf(ApiException.class);
		assertThat(practiceSessionRepository.existsById(sessionId)).isTrue();
	}

	@Test
	void deleteRemovesSessionAndAnswers() {
		UUID userId = userWithCredits(10);
		UUID sessionId = submit(userId, 3).sessionId();

		practiceHistoryCommandService.delete(userId, sessionId);

		assertThat(practiceSessionRepository.existsById(sessionId)).isFalse();
		assertThat(practiceAnswerRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId)).isEmpty();
	}

	private PracticeSessionCommandService.SubmitResult submit(UUID userId, int answerCount) {
		TopicSelection selection = topicSelectionRepository.save(new TopicSelection(userId, UUID.randomUUID()));
		List<PracticeSessionCommandService.AnswerInput> answers = java.util.stream.IntStream.range(0, answerCount)
			.mapToObj(i -> new PracticeSessionCommandService.AnswerInput(UUID.randomUUID(), "Q" + i, "A" + i))
			.toList();
		return practiceSessionCommandService.submit(userId, selection.getId(), answers);
	}

	private UUID userWithCredits(int credits) {
		User user = userRepository.save(new User(
			AuthProvider.KAKAO,
			"test-provider-" + UUID.randomUUID(),
			"test@example.com",
			"test-user",
			UserRole.USER
		));
		creditBalanceService.ensureBalance(user.getId());
		creditBalanceService.addBalance(user.getId(), credits);
		return user.getId();
	}
}
