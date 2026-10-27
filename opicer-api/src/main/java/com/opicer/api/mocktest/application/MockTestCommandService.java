package com.opicer.api.mocktest.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.aijob.application.AiJobCommandService;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.config.WorkerProperties;
import com.opicer.api.credit.application.CreditBalanceCommandService;
import com.opicer.api.credit.application.CreditBalanceQueryService;
import com.opicer.api.mocktest.domain.DifficultyAdjustment;
import com.opicer.api.mocktest.domain.MockTest;
import com.opicer.api.mocktest.domain.MockTestComposer;
import com.opicer.api.mocktest.domain.MockTestComposer.Candidate;
import com.opicer.api.mocktest.domain.MockTestCreditCharge;
import com.opicer.api.mocktest.domain.MockTestItem;
import com.opicer.api.mocktest.domain.MockTestStatus;
import com.opicer.api.mocktest.domain.SurveyCatalog;
import com.opicer.api.mocktest.infrastructure.MockTestCreditChargeRepository;
import com.opicer.api.mocktest.infrastructure.MockTestItemRepository;
import com.opicer.api.mocktest.infrastructure.MockTestRepository;
import com.opicer.api.practice.domain.AnswerEvaluationStatus;
import com.opicer.api.practice.domain.SessionEvaluationStatus;
import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs a mock test from start to submission: survey and self-assessment → part 1 → difficulty re-selection →
 * part 2 → submit, which charges {@value #MOCK_TEST_COST} credits once and queues the evaluation.
 */
@Service
public class MockTestCommandService {

	public static final int MOCK_TEST_COST = 5;
	public static final OpicLevel DEFAULT_TARGET_LEVEL = OpicLevel.IM;
	private static final Set<OpicLevel> TARGET_LEVELS = EnumSet.of(OpicLevel.IM, OpicLevel.IH, OpicLevel.AL);
	private static final TypeReference<Map<String, List<String>>> SURVEY_TYPE = new TypeReference<>() {};

	private final MockTestRepository tests;
	private final MockTestItemRepository items;
	private final MockTestCreditChargeRepository charges;
	private final QuestionPoolLoader pools;
	private final CreditBalanceQueryService creditQuery;
	private final CreditBalanceCommandService creditCommand;
	private final AiJobCommandService jobs;
	private final WorkerProperties workerProperties;
	private final Clock clock;
	private final Random random;
	private final ObjectMapper json = new ObjectMapper();

	public MockTestCommandService(MockTestRepository tests, MockTestItemRepository items,
		MockTestCreditChargeRepository charges, QuestionPoolLoader pools, CreditBalanceQueryService creditQuery,
		CreditBalanceCommandService creditCommand, AiJobCommandService jobs, WorkerProperties workerProperties,
		Clock clock) {
		this.tests = tests;
		this.items = items;
		this.charges = charges;
		this.pools = pools;
		this.creditQuery = creditQuery;
		this.creditCommand = creditCommand;
		this.jobs = jobs;
		this.workerProperties = workerProperties;
		this.clock = clock;
		this.random = new Random();
	}

	/** Checks the survey and the balance (charged only on submit), then composes part 1. */
	@Transactional
	public Composed start(UUID userId, Map<String, List<String>> survey, int selfAssessment, OpicLevel targetLevel) {
		List<String> problems = SurveyCatalog.problems(survey);
		if (!problems.isEmpty()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, String.join(" ", problems));
		}
		if (selfAssessment < 1 || selfAssessment > 6) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "selfAssessment must be 1-6");
		}
		OpicLevel target = targetLevel == null ? DEFAULT_TARGET_LEVEL : targetLevel;
		if (!TARGET_LEVELS.contains(target)) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "targetLevel must be IM, IH or AL");
		}
		if (!creditQuery.hasEnough(userId, MOCK_TEST_COST)) {
			throw new ApiException(ErrorCode.CREDIT_INSUFFICIENT_BALANCE,
				"At least " + MOCK_TEST_COST + " credits are required for a mock test");
		}
		QuestionPoolLoader.LoadedPool pool = pools.load();
		List<Candidate> part1 = new MockTestComposer(random)
			.part1(pool.pool(), SurveyCatalog.topicsOf(survey), MockTestComposer.band(selfAssessment, null));
		if (part1.isEmpty()) {
			throw new ApiException(ErrorCode.MOCK_TEST_NO_QUESTIONS);
		}
		MockTest test = tests.save(new MockTest(userId, write(survey), selfAssessment, target, Instant.now(clock)));
		return new Composed(test, saveItems(test.getId(), part1, 1, 1, pool));
	}

	/** Part 2, once; the difficulty moves the band one step. */
	@Transactional
	public Composed adjust(UUID userId, UUID mockTestId, DifficultyAdjustment adjustment) {
		MockTest test = tests.findLockedByIdAndUserId(mockTestId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.MOCK_TEST_NOT_FOUND));
		if (test.getStatus() != MockTestStatus.IN_PROGRESS) {
			throw new ApiException(ErrorCode.MOCK_TEST_NOT_IN_PROGRESS);
		}
		if (test.isAdjusted()) {
			throw new ApiException(ErrorCode.MOCK_TEST_ALREADY_ADJUSTED);
		}
		QuestionPoolLoader.LoadedPool pool = pools.load();
		List<MockTestItem> part1 = items.findByMockTestIdOrderByItemOrderAsc(test.getId());
		List<Candidate> asked = part1.stream().map(item -> asCandidate(item, pool)).toList();
		List<Candidate> part2 = new MockTestComposer(random).part2(pool.pool(), SurveyCatalog.topicsOf(read(test.getSurvey())),
			asked, MockTestComposer.band(test.getSelfAssessment(), adjustment));
		test.adjust(adjustment);
		return new Composed(test, saveItems(test.getId(), part2, part1.size() + 1, 2, pool));
	}

	/**
	 * Records the answers, charges once and queues the evaluation. Repeating a submit returns the first result
	 * without charging again. Questions without an answer are kept and skipped by the evaluation.
	 */
	@Transactional
	public Submitted submit(UUID userId, UUID mockTestId, List<AnswerInput> answers, Integer examSeconds) {
		MockTest test = tests.findLockedByIdAndUserId(mockTestId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.MOCK_TEST_NOT_FOUND));
		MockTestCreditCharge existing = charges.findByMockTestId(test.getId()).orElse(null);
		if (existing != null) {
			return new Submitted(existing.getId(), true, existing.getAmount(), test.getId(), test.getEvaluationStatus());
		}
		Map<UUID, MockTestItem> byId = items.findByMockTestIdOrderByItemOrderAsc(test.getId()).stream()
			.collect(Collectors.toMap(MockTestItem::getId, Function.identity()));
		Map<UUID, AnswerInput> answered = (answers == null ? List.<AnswerInput>of() : answers).stream()
			.collect(Collectors.toMap(AnswerInput::itemId, Function.identity(), (a, b) -> {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "Duplicate itemId in answers");
			}));
		if (!byId.keySet().containsAll(answered.keySet())) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "An answer refers to a question outside this mock test");
		}
		if (!creditCommand.deductIfEnough(userId, MOCK_TEST_COST)) {
			throw new ApiException(ErrorCode.CREDIT_INSUFFICIENT_BALANCE,
				"At least " + MOCK_TEST_COST + " credits are required to submit a mock test");
		}
		MockTestCreditCharge charge = charges.save(new MockTestCreditCharge(userId, test.getId(), MOCK_TEST_COST));
		for (MockTestItem item : byId.values()) {
			AnswerInput answer = answered.get(item.getId());
			item.answer(answer == null ? null : answer.transcript(), answer == null ? null : answer.durationSeconds());
		}
		test.submit(examSeconds, Instant.now(clock));
		// Same transaction: the job exists only if the submission commits.
		jobs.enqueue(AiJobType.MOCK_TEST_EVALUATION, test.getId());
		return new Submitted(charge.getId(), false, MOCK_TEST_COST, test.getId(), test.getEvaluationStatus());
	}

	/** Free, limited per test; only the answers that failed are evaluated again. */
	@Transactional
	public SessionEvaluationStatus retry(UUID userId, UUID mockTestId) {
		MockTest test = tests.findByIdAndUserId(mockTestId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.MOCK_TEST_NOT_FOUND));
		SessionEvaluationStatus status = test.getEvaluationStatus();
		if (status != SessionEvaluationStatus.FAILED && status != SessionEvaluationStatus.PARTIAL) {
			throw new ApiException(ErrorCode.EVALUATION_NOT_RETRYABLE);
		}
		if (!jobs.requeueManually(AiJobType.MOCK_TEST_EVALUATION, test.getId(), workerProperties.getMaxManualRetries())) {
			throw new ApiException(ErrorCode.EVALUATION_RETRY_LIMIT);
		}
		for (MockTestItem item : items.findByMockTestIdOrderByItemOrderAsc(test.getId())) {
			if (item.getEvaluationStatus() == AnswerEvaluationStatus.FAILED) {
				item.markEvaluationPending();
			}
		}
		test.requestEvaluation();
		return test.getEvaluationStatus();
	}

	/** Physically deletes the test, its answers and its evaluation job. The credit record stays. */
	@Transactional
	public void delete(UUID userId, UUID mockTestId) {
		MockTest test = tests.findByIdAndUserId(mockTestId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.MOCK_TEST_NOT_FOUND));
		jobs.delete(AiJobType.MOCK_TEST_EVALUATION, test.getId());
		items.deleteByMockTestId(test.getId());
		tests.delete(test);
	}

	private List<MockTestItem> saveItems(UUID mockTestId, List<Candidate> picked, int firstOrder, int part,
		QuestionPoolLoader.LoadedPool pool) {
		List<MockTestItem> saved = new ArrayList<>();
		for (int i = 0; i < picked.size(); i++) {
			Candidate candidate = picked.get(i);
			QuestionPoolLoader.QuestionInfo info = pool.info(candidate.id());
			saved.add(new MockTestItem(mockTestId, firstOrder + i, part, candidate.id(), info.promptText(),
				candidate.type() == null ? null : candidate.type().name(), info.promptAudioUrl(), candidate.topic(),
				pool.topicId(candidate.topic())));
		}
		return items.saveAll(saved);
	}

	/** A part-1 item as the composer sees it, even if its question has since been edited or removed. */
	private static Candidate asCandidate(MockTestItem item, QuestionPoolLoader.LoadedPool pool) {
		return pool.candidate(item.getQuestionId()).orElseGet(() -> new Candidate(item.getQuestionId(), item.getTopicTitle(),
			item.getQuestionType() == null ? null : QuestionType.valueOf(item.getQuestionType()), Set.of(), -1));
	}

	private String write(Map<String, List<String>> survey) {
		try {
			return json.writeValueAsString(survey);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("survey is not serialisable", e);
		}
	}

	static Map<String, List<String>> read(String surveyJson) {
		try {
			return new ObjectMapper().readValue(surveyJson, SURVEY_TYPE);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("stored survey is not valid JSON", e);
		}
	}

	/** @param durationSeconds recording length; null when unknown */
	public record AnswerInput(UUID itemId, String transcript, Integer durationSeconds) {}

	/** A test with the items just added to it (part 1 on start, part 2 on adjust). */
	public record Composed(MockTest test, List<MockTestItem> items) {}

	public record Submitted(UUID chargeId, boolean alreadyCharged, int deductedCredits, UUID mockTestId,
		SessionEvaluationStatus evaluationStatus) {}
}
