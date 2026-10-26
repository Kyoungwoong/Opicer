package com.opicer.api.practice.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.aijob.application.AiJobCommandService;
import com.opicer.api.aijob.application.AiJobRunner;
import com.opicer.api.aijob.domain.AiJob;
import com.opicer.api.aijob.domain.AiJobStatus;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.aijob.infrastructure.AiJobRepository;
import com.opicer.api.evaluation.application.AnswerEvaluationModel;
import com.opicer.api.evaluation.application.EvaluationFailure;
import com.opicer.api.evaluation.application.EvaluationPrompt;
import com.opicer.api.evaluation.domain.EvaluationGuideKind;
import com.opicer.api.evaluationguide.application.EvaluationGuideCommandService;
import com.opicer.api.evaluationguide.infrastructure.EvaluationGuideRepository;
import com.opicer.api.practice.domain.AnswerEvaluationStatus;
import com.opicer.api.practice.domain.PracticeAnswer;
import com.opicer.api.practice.domain.PracticeSession;
import com.opicer.api.practice.domain.SessionEvaluationStatus;
import com.opicer.api.practice.infrastructure.PracticeAnswerRepository;
import com.opicer.api.practice.infrastructure.PracticeSessionRepository;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import static com.opicer.api.evaluation.application.EvaluationFixtures.TRANSCRIPT;
import static com.opicer.api.evaluation.application.EvaluationFixtures.validOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Submit → job → worker → per-answer evaluation → session report, on real Postgres. The evaluation model is
 * scripted per answer: transcripts carry a marker the fake model reacts to.
 */
@SpringBootTest
class SessionEvaluationFlowTest {

	private static final String OK = TRANSCRIPT + " Marker ok.";
	private static final String FLAKY = TRANSCRIPT + " Marker flaky.";
	private static final String REFUSED = TRANSCRIPT + " Marker refused.";
	private static final String SHORT = "I like my home a lot.";

	@Autowired
	private AiJobRunner runner;

	@Autowired
	private AiJobCommandService aiJobCommandService;

	@Autowired
	private AiJobRepository aiJobRepository;

	@Autowired
	private PracticeSessionRepository sessionRepository;

	@Autowired
	private PracticeAnswerRepository answerRepository;

	@Autowired
	private PracticeEvaluationCommandService evaluationCommandService;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private SessionEvaluationProcessor processor;

	@Autowired
	private EvaluationGuideCommandService guideCommandService;

	@Autowired
	private EvaluationGuideRepository guideRepository;

	@MockBean
	private AnswerEvaluationModel model;

	private final List<EvaluationPrompt> prompts = new CopyOnWriteArrayList<>();

	/** Calls per marker, and how many leading calls of FLAKY should fail. */
	private final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
	private volatile int flakyFailures = 1;

	@BeforeEach
	void setUp() {
		aiJobRepository.deleteAll();
		answerRepository.deleteAll();
		sessionRepository.deleteAll();
		guideRepository.deleteAll();
		when(model.provider()).thenReturn("fake");
		when(model.model()).thenReturn("fake-model");
		when(model.evaluate(any())).thenAnswer(invocation -> {
			prompts.add(invocation.getArgument(0));
			String input = invocation.<EvaluationPrompt>getArgument(0).input();
			String marker = input.contains("Marker flaky") ? "flaky" : input.contains("Marker refused") ? "refused" : "ok";
			int n = calls.computeIfAbsent(marker, m -> new AtomicInteger()).incrementAndGet();
			if (marker.equals("refused")) {
				throw EvaluationFailure.permanent("REFUSED", "declined");
			}
			if (marker.equals("flaky") && n <= flakyFailures) {
				throw EvaluationFailure.retryable("RATE_LIMITED", null);
			}
			return validOutput();
		});
	}

	@Test
	void evaluatesEveryAnswerAndWritesTheSessionReport() throws Exception {
		UUID sessionId = session(OpicLevel.IH, OK, OK, SHORT);

		runner.runOnce();

		PracticeSession session = sessionRepository.findById(sessionId).orElseThrow();
		assertThat(session.getEvaluationStatus()).isEqualTo(SessionEvaluationStatus.DONE);
		assertThat(session.getEstimatedLevel()).isEqualTo(OpicLevel.IM);
		assertThat(session.getEvaluatedAt()).isNotNull();
		JsonNode report = new ObjectMapper().readTree(session.getReport());
		assertThat(report.path("estimatedLevel").asText()).isEqualTo("IM");
		assertThat(report.path("targetLevel").asText()).isEqualTo("IH");
		assertThat(report.path("gapToTarget").asInt()).isEqualTo(1);
		assertThat(report.path("evaluatedAnswers").asInt()).isEqualTo(2);
		assertThat(report.path("skippedAnswers").asInt()).isEqualTo(1);
		assertThat(report.path("topIssues").get(0).path("category").asText()).isEqualTo("TENSE");
		assertThat(report.path("disclaimer").asText()).contains("공식 OPIc 점수가 아닙니다");

		List<PracticeAnswer> answers = answers(sessionId);
		assertThat(answers).extracting(PracticeAnswer::getEvaluationStatus).containsExactly(
			AnswerEvaluationStatus.DONE, AnswerEvaluationStatus.DONE, AnswerEvaluationStatus.SKIPPED);
		assertThat(answers.get(0).getOverallLevel()).isEqualTo(OpicLevel.IM);
		assertThat(answers.get(0).getAnalysis()).contains("예상 레벨: IM");
		assertThat(answers.get(0).getImprovedScript()).startsWith("Last summer, I went");
		assertThat(new ObjectMapper().readTree(answers.get(0).getEvaluation()).path("schemaVersion").asInt()).isEqualTo(1);
		assertThat(job(sessionId).getStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
	}

	@Test
	void activeGuidesReachTheModelAndAreRecordedInTheEvaluation() throws Exception {
		guideCommandService.create(EvaluationGuideKind.TIPS, "팁 초안", "- old tip");
		guideCommandService.activate(guideCommandService.create(EvaluationGuideKind.TIPS, "팁", "- 연결어를 칭찬해 주세요").getId());
		UUID sessionId = session(OpicLevel.IM, OK);

		runner.runOnce();

		assertThat(prompts).hasSize(1);
		assertThat(prompts.get(0).instructions())
			.contains("<admin_guide kind=\"TIPS\" version=\"2\">")
			.contains("- 연결어를 칭찬해 주세요")
			.doesNotContain("old tip");
		JsonNode guides = new ObjectMapper().readTree(answers(sessionId).get(0).getEvaluation()).path("meta").path("guides");
		assertThat(guides).hasSize(1);
		assertThat(guides.get(0).path("kind").asText()).isEqualTo("TIPS");
		assertThat(guides.get(0).path("version").asInt()).isEqualTo(2);
	}

	@Test
	void transientFailureRetriesOnlyTheFailedAnswer() {
		UUID sessionId = session(OpicLevel.IM, OK, FLAKY);

		runner.runOnce();
		assertThat(sessionRepository.findById(sessionId).orElseThrow().getEvaluationStatus())
			.isEqualTo(SessionEvaluationStatus.RUNNING);
		assertThat(answers(sessionId)).extracting(PracticeAnswer::getEvaluationStatus)
			.containsExactly(AnswerEvaluationStatus.DONE, AnswerEvaluationStatus.PENDING);
		assertThat(job(sessionId).getStatus()).isEqualTo(AiJobStatus.READY);

		makeDue(sessionId);
		runner.runOnce();

		assertThat(sessionRepository.findById(sessionId).orElseThrow().getEvaluationStatus())
			.isEqualTo(SessionEvaluationStatus.DONE);
		assertThat(calls.get("ok").get()).as("finished answer is not evaluated again").isEqualTo(1);
		assertThat(calls.get("flaky").get()).isEqualTo(2);
	}

	@Test
	void exhaustedRetriesEndPartialAndPermanentFailuresDoNotRetry() {
		flakyFailures = 99;
		UUID flaky = session(OpicLevel.IM, OK, FLAKY);
		UUID refused = session(OpicLevel.IM, OK, REFUSED);

		runner.runOnce();
		// The refusal settles at once: no retry for that session.
		assertThat(sessionRepository.findById(refused).orElseThrow().getEvaluationStatus())
			.isEqualTo(SessionEvaluationStatus.PARTIAL);
		assertThat(calls.get("refused").get()).isEqualTo(1);

		makeDue(flaky);
		runner.runOnce();
		makeDue(flaky);
		runner.runOnce();

		PracticeSession partial = sessionRepository.findById(flaky).orElseThrow();
		assertThat(partial.getEvaluationStatus()).isEqualTo(SessionEvaluationStatus.PARTIAL);
		assertThat(partial.getEstimatedLevel()).isEqualTo(OpicLevel.IM);
		assertThat(answers(flaky)).extracting(PracticeAnswer::getEvaluationStatus)
			.containsExactly(AnswerEvaluationStatus.DONE, AnswerEvaluationStatus.FAILED);
		assertThat(calls.get("flaky").get()).isEqualTo(3);
	}

	@Test
	void abandonedSessionIsSettledInsteadOfStayingRunning() {
		flakyFailures = 99;
		UUID sessionId = session(OpicLevel.IM, OK, FLAKY);
		runner.runOnce();
		assertThat(sessionRepository.findById(sessionId).orElseThrow().getEvaluationStatus())
			.isEqualTo(SessionEvaluationStatus.RUNNING);

		processor.abandon(sessionId);

		PracticeSession settled = sessionRepository.findById(sessionId).orElseThrow();
		assertThat(settled.getEvaluationStatus()).isEqualTo(SessionEvaluationStatus.PARTIAL);
		assertThat(settled.getReport()).isNotNull();
		assertThat(answers(sessionId)).extracting(PracticeAnswer::getEvaluationStatus)
			.containsExactly(AnswerEvaluationStatus.DONE, AnswerEvaluationStatus.FAILED);
	}

	@Test
	void nothingEvaluableEndsFailedWithoutAReport() {
		UUID sessionId = session(OpicLevel.IM, SHORT, SHORT);

		runner.runOnce();

		PracticeSession session = sessionRepository.findById(sessionId).orElseThrow();
		assertThat(session.getEvaluationStatus()).isEqualTo(SessionEvaluationStatus.FAILED);
		assertThat(session.getReport()).isNull();
		assertThat(calls).isEmpty();
	}

	@Test
	void sessionDeletedBeforeTheWorkerRunsIsANoOp() {
		UUID sessionId = session(OpicLevel.IM, OK);
		answerRepository.deleteAll();
		sessionRepository.deleteById(sessionId);

		runner.runOnce();

		assertThat(job(sessionId).getStatus()).isEqualTo(AiJobStatus.SUCCEEDED);
		assertThat(calls).isEmpty();
	}

	@Test
	void learnerRetryReEvaluatesOnlyFailedAnswersWithinTheLimit() {
		UUID userId = UUID.randomUUID();
		UUID sessionId = session(userId, OpicLevel.IM, OK, REFUSED);
		runner.runOnce();
		assertThat(sessionRepository.findById(sessionId).orElseThrow().getEvaluationStatus())
			.isEqualTo(SessionEvaluationStatus.PARTIAL);

		assertThat(evaluationCommandService.retry(userId, sessionId)).isEqualTo(SessionEvaluationStatus.PENDING);
		assertThat(answers(sessionId)).extracting(PracticeAnswer::getEvaluationStatus)
			.containsExactly(AnswerEvaluationStatus.DONE, AnswerEvaluationStatus.PENDING);
		runner.runOnce();
		assertThat(calls.get("ok").get()).isEqualTo(1);
		assertThat(calls.get("refused").get()).isEqualTo(2);

		evaluationCommandService.retry(userId, sessionId);
		runner.runOnce();
		assertThatThrownBy(() -> evaluationCommandService.retry(userId, sessionId))
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EVALUATION_RETRY_LIMIT));
	}

	@Test
	void retryIsRefusedForFinishedOrForeignSessions() {
		UUID userId = UUID.randomUUID();
		UUID sessionId = session(userId, OpicLevel.IM, OK);
		runner.runOnce();

		assertThatThrownBy(() -> evaluationCommandService.retry(userId, sessionId))
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EVALUATION_NOT_RETRYABLE));
		assertThatThrownBy(() -> evaluationCommandService.retry(UUID.randomUUID(), sessionId))
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PRACTICE_SESSION_NOT_FOUND));
	}

	private UUID session(OpicLevel target, String... transcripts) {
		return session(UUID.randomUUID(), target, transcripts);
	}

	/** A submitted session as PracticeSessionCommandService leaves it: PENDING answers and a queued job. */
	private UUID session(UUID userId, OpicLevel target, String... transcripts) {
		PracticeSession session = new PracticeSession(userId, UUID.randomUUID(), UUID.randomUUID(), "여행", null);
		session.requestEvaluation(target);
		session = sessionRepository.save(session);
		List<PracticeAnswer> answers = new ArrayList<>();
		for (int i = 0; i < transcripts.length; i++) {
			PracticeAnswer answer = new PracticeAnswer(session.getId(), UUID.randomUUID(), i, "Tell me about a trip.",
				transcripts[i], 60);
			answer.markEvaluationPending();
			answers.add(answer);
		}
		answerRepository.saveAll(answers);
		aiJobCommandService.enqueue(AiJobType.SESSION_EVALUATION, session.getId());
		return session.getId();
	}

	private List<PracticeAnswer> answers(UUID sessionId) {
		return answerRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);
	}

	private AiJob job(UUID sessionId) {
		return aiJobRepository.findByDedupeKey(AiJob.dedupeKey(AiJobType.SESSION_EVALUATION, sessionId)).orElseThrow();
	}

	private void makeDue(UUID sessionId) {
		jdbc.update("update ai_job set next_run_at = now() - interval '1 second' where subject_id = ?", sessionId);
	}
}
