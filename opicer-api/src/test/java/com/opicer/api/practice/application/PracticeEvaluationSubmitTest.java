package com.opicer.api.practice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.aijob.application.AiJobCommandService;
import com.opicer.api.aijob.domain.AiJob;
import com.opicer.api.aijob.domain.AiJobStatus;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.aijob.infrastructure.AiJobRepository;
import com.opicer.api.credit.application.CreditBalanceCommandService;
import com.opicer.api.credit.application.CreditBalanceQueryService;
import com.opicer.api.practice.application.PracticeSessionCommandService.AnswerInput;
import com.opicer.api.practice.application.PracticeSessionCommandService.SubmitResult;
import com.opicer.api.practice.domain.AnswerEvaluationStatus;
import com.opicer.api.practice.domain.PracticeAnswer;
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

/** Submit records evaluation state and queues exactly one evaluation job, on real Postgres. */
@SpringBootTest
class PracticeEvaluationSubmitTest {

	@Autowired
	private PracticeSessionCommandService submitService;

	@Autowired
	private PracticeHistoryCommandService historyCommandService;

	@Autowired
	private PracticeHistoryQueryService historyQueryService;

	@Autowired
	private AiJobCommandService aiJobCommandService;

	@Autowired
	private AiJobRepository aiJobRepository;

	@Autowired
	private PracticeSessionRepository sessionRepository;

	@Autowired
	private PracticeAnswerRepository answerRepository;

	@Autowired
	private PracticeCreditChargeRepository chargeRepository;

	@Autowired
	private TopicSelectionRepository selectionRepository;

	@Autowired
	private CreditBalanceCommandService creditBalanceCommandService;

	@Autowired
	private CreditBalanceQueryService creditBalanceQueryService;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		aiJobRepository.deleteAll();
		answerRepository.deleteAll();
		sessionRepository.deleteAll();
		chargeRepository.deleteAll();
		selectionRepository.deleteAll();
	}

	@Test
	void submitDefaultsToImAndQueuesOneEvaluationJob() {
		UUID userId = userWithCredits(10);
		UUID selectionId = selection(userId);

		SubmitResult result = submitService.submit(userId, selectionId, null, List.of(
			new AnswerInput(UUID.randomUUID(), "Describe your home.", "I live in an apartment.", 74),
			new AnswerInput(UUID.randomUUID(), "Tell me about a trip.", null, null)
		));

		assertThat(result.evaluationStatus()).isEqualTo(SessionEvaluationStatus.PENDING);
		PracticeSession session = sessionRepository.findById(result.sessionId()).orElseThrow();
		assertThat(session.getTargetLevel()).isEqualTo(OpicLevel.IM);
		assertThat(session.getEvaluationStatus()).isEqualTo(SessionEvaluationStatus.PENDING);
		assertThat(session.getEstimatedLevel()).isNull();
		assertThat(session.getReport()).isNull();

		List<PracticeAnswer> answers = answerRepository.findBySessionIdOrderByQuestionOrderAsc(session.getId());
		assertThat(answers).extracting(PracticeAnswer::getEvaluationStatus)
			.containsOnly(AnswerEvaluationStatus.PENDING);
		assertThat(answers).extracting(PracticeAnswer::getDurationSeconds).containsExactly(74, null);

		AiJob job = aiJobRepository.findByDedupeKey(AiJob.dedupeKey(AiJobType.SESSION_EVALUATION, session.getId()))
			.orElseThrow();
		assertThat(job.getStatus()).isEqualTo(AiJobStatus.READY);
		assertThat(job.getSubjectId()).isEqualTo(session.getId());
		assertThat(job.getAttempts()).isZero();
	}

	@Test
	void resubmittingKeepsOneChargeOneSessionOneJob() {
		UUID userId = userWithCredits(10);
		UUID selectionId = selection(userId);
		List<AnswerInput> answers = List.of(new AnswerInput(UUID.randomUUID(), "Q", "A long enough answer", 30));

		SubmitResult first = submitService.submit(userId, selectionId, OpicLevel.IH, answers);
		SubmitResult second = submitService.submit(userId, selectionId, OpicLevel.IH, answers);

		assertThat(second.sessionId()).isEqualTo(first.sessionId());
		assertThat(second.alreadyCharged()).isTrue();
		assertThat(aiJobRepository.count()).isEqualTo(1);
		assertThat(chargeRepository.count()).isEqualTo(1);
		assertThat(creditBalanceQueryService.getBalance(userId).getBalance()).isEqualTo(8);
		assertThat(sessionRepository.findById(first.sessionId()).orElseThrow().getTargetLevel()).isEqualTo(OpicLevel.IH);
	}

	@Test
	void levelsBelowImAreNotTargetsAndChargeNothing() {
		UUID userId = userWithCredits(10);
		UUID selectionId = selection(userId);

		for (OpicLevel level : List.of(OpicLevel.NL, OpicLevel.IL)) {
			assertThatThrownBy(() -> submitService.submit(userId, selectionId, level, List.of()))
				.isInstanceOfSatisfying(ApiException.class,
					e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
		}
		assertThat(chargeRepository.count()).isZero();
		assertThat(creditBalanceQueryService.getBalance(userId).getBalance()).isEqualTo(10);
	}

	@Test
	void sessionWithoutAnswersHasNothingToEvaluate() {
		UUID userId = userWithCredits(10);

		SubmitResult result = submitService.submit(userId, selection(userId), OpicLevel.AL, List.of());

		assertThat(result.evaluationStatus()).isNull();
		assertThat(sessionRepository.findById(result.sessionId()).orElseThrow().getTargetLevel()).isNull();
		assertThat(aiJobRepository.count()).isZero();
	}

	@Test
	void deletingTheSessionDeletesItsJob() {
		UUID userId = userWithCredits(10);
		SubmitResult result = submitService.submit(userId, selection(userId), null,
			List.of(new AnswerInput(UUID.randomUUID(), "Q", "A", null)));

		historyCommandService.delete(userId, result.sessionId());

		assertThat(aiJobRepository.count()).isZero();
		assertThat(sessionRepository.count()).isZero();
	}

	@Test
	void enqueueIsIdempotentPerSubject() {
		UUID subject = UUID.randomUUID();

		assertThat(aiJobCommandService.enqueue(AiJobType.SESSION_EVALUATION, subject)).isTrue();
		assertThat(aiJobCommandService.enqueue(AiJobType.SESSION_EVALUATION, subject)).isFalse();
		assertThat(aiJobRepository.countByDedupeKey(AiJob.dedupeKey(AiJobType.SESSION_EVALUATION, subject))).isEqualTo(1);
	}

	@Test
	void legacySessionsWithoutEvaluationColumnsStillLoad() {
		UUID userId = userWithCredits(0);
		PracticeSession legacy = sessionRepository.save(
			new PracticeSession(userId, UUID.randomUUID(), UUID.randomUUID(), "집", null));
		answerRepository.save(new PracticeAnswer(legacy.getId(), UUID.randomUUID(), 0, "Q", "A"));

		PracticeHistoryQueryService.SessionDetail detail = historyQueryService.get(userId, legacy.getId());

		assertThat(detail.session().getEvaluationStatus()).isNull();
		assertThat(detail.answers().get(0).getEvaluationStatus()).isNull();
		assertThat(detail.answers().get(0).getEvaluation()).isNull();
	}

	@Test
	void evaluationJsonRoundTripsThroughJsonbColumns() throws Exception {
		UUID userId = userWithCredits(0);
		PracticeSession session = new PracticeSession(userId, UUID.randomUUID(), UUID.randomUUID(), "집", null);
		session.requestEvaluation(OpicLevel.IH);
		session.completeEvaluation(SessionEvaluationStatus.DONE, OpicLevel.IM,
			"{\"estimatedLevel\": \"IM\", \"tips\": [\"연결어를 늘려 보세요\"], \"confidence\": \"LOW\"}");
		session = sessionRepository.save(session);
		PracticeAnswer answer = new PracticeAnswer(session.getId(), UUID.randomUUID(), 0, "Q", "I go to park.", 40);
		answer.recordEvaluation("{\"dimensions\": [{\"code\": \"ACCURACY\", \"level\": \"IL\"}], \"schemaVersion\": 1}",
			OpicLevel.IL, "요약", "I went to the park.");
		answerRepository.save(answer);

		// Fresh reads from the database (no shared persistence context in this test).
		PracticeSession reloaded = sessionRepository.findById(session.getId()).orElseThrow();
		PracticeAnswer reloadedAnswer = answerRepository.findBySessionIdOrderByQuestionOrderAsc(session.getId()).get(0);
		ObjectMapper json = new ObjectMapper();

		assertThat(reloaded.getEvaluationStatus()).isEqualTo(SessionEvaluationStatus.DONE);
		assertThat(reloaded.getEstimatedLevel()).isEqualTo(OpicLevel.IM);
		assertThat(reloaded.getEvaluatedAt()).isNotNull();
		// jsonb normalises key order and spacing, so compare as JSON trees.
		assertThat(json.readTree(reloaded.getReport())).isEqualTo(json.readTree(
			"{\"confidence\":\"LOW\",\"estimatedLevel\":\"IM\",\"tips\":[\"연결어를 늘려 보세요\"]}"));
		assertThat(json.readTree(reloadedAnswer.getEvaluation()).at("/dimensions/0/level").asText()).isEqualTo("IL");
		assertThat(reloadedAnswer.getEvaluationStatus()).isEqualTo(AnswerEvaluationStatus.DONE);
		assertThat(reloadedAnswer.getOverallLevel()).isEqualTo(OpicLevel.IL);
		assertThat(reloadedAnswer.getImprovedScript()).isEqualTo("I went to the park.");
	}

	@Test
	void completeEvaluationRejectsNonFinalStatuses() {
		PracticeSession session = new PracticeSession(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "집", null);

		assertThatThrownBy(() -> session.completeEvaluation(SessionEvaluationStatus.RUNNING, null, null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private UUID selection(UUID userId) {
		return selectionRepository.save(new TopicSelection(userId, UUID.randomUUID())).getId();
	}

	private UUID userWithCredits(int credits) {
		UUID userId = userRepository.save(new User(AuthProvider.KAKAO, "eval-" + UUID.randomUUID(),
			"eval@example.com", "eval-user", UserRole.USER)).getId();
		creditBalanceCommandService.ensureBalance(userId);
		if (credits > 0) {
			creditBalanceCommandService.addBalance(userId, credits);
		}
		return userId;
	}
}
