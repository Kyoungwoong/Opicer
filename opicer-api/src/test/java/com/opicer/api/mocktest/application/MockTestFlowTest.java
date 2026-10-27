package com.opicer.api.mocktest.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.aijob.application.AiJobRunner;
import com.opicer.api.aijob.domain.AiJobType;
import com.opicer.api.aijob.infrastructure.AiJobRepository;
import com.opicer.api.credit.application.CreditBalanceCommandService;
import com.opicer.api.credit.application.CreditBalanceQueryService;
import com.opicer.api.evaluation.application.AnswerEvaluationModel;
import com.opicer.api.evaluation.application.EvaluationPrompt;
import com.opicer.api.evaluation.domain.EvaluationMode;
import com.opicer.api.mocktest.application.MockTestCommandService.AnswerInput;
import com.opicer.api.mocktest.application.MockTestCommandService.Composed;
import com.opicer.api.mocktest.application.MockTestCommandService.Submitted;
import com.opicer.api.mocktest.domain.DifficultyAdjustment;
import com.opicer.api.mocktest.domain.MockTest;
import com.opicer.api.mocktest.domain.MockTestItem;
import com.opicer.api.mocktest.domain.MockTestStatus;
import com.opicer.api.mocktest.infrastructure.MockTestCreditChargeRepository;
import com.opicer.api.mocktest.infrastructure.MockTestItemRepository;
import com.opicer.api.mocktest.infrastructure.MockTestRepository;
import com.opicer.api.practice.domain.AnswerEvaluationStatus;
import com.opicer.api.practice.domain.SessionEvaluationStatus;
import com.opicer.api.question.domain.Question;
import com.opicer.api.question.domain.QuestionType;
import com.opicer.api.question.infrastructure.QuestionRepository;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import com.opicer.api.user.domain.AuthProvider;
import com.opicer.api.user.domain.User;
import com.opicer.api.user.domain.UserRole;
import com.opicer.api.user.infrastructure.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.opicer.api.evaluation.application.EvaluationFixtures.TRANSCRIPT;
import static com.opicer.api.evaluation.application.EvaluationFixtures.validOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Start → adjust → submit → worker → report, on real Postgres with a scripted evaluation model. */
@SpringBootTest
class MockTestFlowTest {

	@Autowired
	private MockTestCommandService commandService;

	@Autowired
	private MockTestQueryService queryService;

	@Autowired
	private MockTestRepository testRepository;

	@Autowired
	private MockTestItemRepository itemRepository;

	@Autowired
	private MockTestCreditChargeRepository chargeRepository;

	@Autowired
	private AiJobRepository jobRepository;

	@Autowired
	private AiJobRunner runner;

	@Autowired
	private QuestionRepository questionRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CreditBalanceCommandService creditCommand;

	@Autowired
	private CreditBalanceQueryService creditQuery;

	@MockBean
	private AnswerEvaluationModel model;

	private final List<EvaluationPrompt> prompts = new CopyOnWriteArrayList<>();

	@BeforeEach
	void setUp() {
		jobRepository.deleteAll();
		itemRepository.deleteAll();
		chargeRepository.deleteAll();
		testRepository.deleteAll();
		ensureTopic("음악 감상", "설문조사");
		ensureTopic("영화 감상", "설문조사");
		ensureTopic("물건 분실", "돌발질문");
		when(model.provider()).thenReturn("fake");
		when(model.model()).thenReturn("fake-model");
		when(model.evaluate(any())).thenAnswer(invocation -> {
			prompts.add(invocation.getArgument(0));
			return validOutput();
		});
	}

	@Test
	void startComposesPartOneWithoutCharging() {
		UUID userId = userWithCredits(10);

		Composed started = commandService.start(userId, survey(), 4, OpicLevel.IH);

		assertThat(started.items()).hasSize(7);
		assertThat(started.items()).extracting(MockTestItem::getItemOrder).containsExactly(1, 2, 3, 4, 5, 6, 7);
		assertThat(started.items()).allMatch(i -> i.getPart() == 1);
		assertThat(started.items().get(0).getTopicTitle()).isEqualTo("자기소개");
		assertThat(started.test().getStatus()).isEqualTo(MockTestStatus.IN_PROGRESS);
		assertThat(creditQuery.getBalance(userId).getBalance()).isEqualTo(10);
	}

	@Test
	void anIncompleteSurveyOrTooFewCreditsIsRefused() {
		UUID rich = userWithCredits(10);
		Map<String, List<String>> incomplete = survey();
		incomplete.put("SPORT", List.of());
		UUID poor = userWithCredits(4);

		assertThatThrownBy(() -> commandService.start(rich, incomplete, 4, OpicLevel.IM))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
		assertThatThrownBy(() -> commandService.start(poor, survey(), 4, OpicLevel.IM))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.CREDIT_INSUFFICIENT_BALANCE);
		assertThat(testRepository.count()).isZero();
	}

	@Test
	void adjustAddsPartTwoOnlyOnce() {
		UUID userId = userWithCredits(10);
		UUID id = commandService.start(userId, survey(), 4, OpicLevel.IM).test().getId();

		Composed part2 = commandService.adjust(userId, id, DifficultyAdjustment.HARDER);

		assertThat(part2.items()).hasSize(8);
		assertThat(part2.items()).extracting(MockTestItem::getItemOrder).containsExactly(8, 9, 10, 11, 12, 13, 14, 15);
		assertThat(part2.items().get(3).getTopicTitle()).startsWith("롤플레이:");
		assertThat(part2.items().subList(6, 8)).allMatch(i -> i.getTopicTitle().startsWith("고난도:"));
		List<UUID> questionIds = itemRepository.findByMockTestIdOrderByItemOrderAsc(id).stream()
			.map(MockTestItem::getQuestionId).toList();
		assertThat(questionIds).doesNotHaveDuplicates().hasSize(15);
		assertThatThrownBy(() -> commandService.adjust(userId, id, DifficultyAdjustment.EASIER))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.MOCK_TEST_ALREADY_ADJUSTED);
	}

	@Test
	void submitChargesFiveOnceAndTheWorkerWritesAFullReport() throws Exception {
		UUID userId = userWithCredits(10);
		UUID id = commandService.start(userId, survey(), 4, OpicLevel.IH).test().getId();
		commandService.adjust(userId, id, DifficultyAdjustment.SAME);
		List<MockTestItem> items = itemRepository.findByMockTestIdOrderByItemOrderAsc(id);
		List<AnswerInput> answers = new ArrayList<>();
		for (int i = 0; i < 13; i++) {
			answers.add(new AnswerInput(items.get(i).getId(), TRANSCRIPT, 60));
		}
		answers.add(new AnswerInput(items.get(13).getId(), "Too short.", 5));

		Submitted first = commandService.submit(userId, id, answers, 1800);
		Submitted again = commandService.submit(userId, id, answers, 1800);

		assertThat(first.alreadyCharged()).isFalse();
		assertThat(first.deductedCredits()).isEqualTo(5);
		assertThat(again.alreadyCharged()).isTrue();
		assertThat(creditQuery.getBalance(userId).getBalance()).isEqualTo(5);
		assertThat(jobRepository.findAll()).extracting(j -> j.getType()).containsExactly(AiJobType.MOCK_TEST_EVALUATION);
		assertThatThrownBy(() -> commandService.adjust(userId, id, DifficultyAdjustment.SAME))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.MOCK_TEST_NOT_IN_PROGRESS);

		runner.runOnce();

		MockTest test = testRepository.findById(id).orElseThrow();
		assertThat(test.getEvaluationStatus()).isEqualTo(SessionEvaluationStatus.DONE);
		assertThat(test.getEstimatedLevel()).isEqualTo(OpicLevel.IM);
		assertThat(test.getExamSeconds()).isEqualTo(1800);
		JsonNode report = new ObjectMapper().readTree(test.getReport());
		assertThat(report.path("evaluatedAnswers").asInt()).isEqualTo(13);
		assertThat(report.path("skippedAnswers").asInt()).isEqualTo(2);
		assertThat(report.path("targetLevel").asText()).isEqualTo("IH");
		assertThat(report.path("disclaimer").asText()).contains("공식 OPIc 점수가 아닙니다");
		assertThat(prompts).hasSize(13).allSatisfy(p -> assertThat(p.mode()).isEqualTo(EvaluationMode.FULL));
		List<MockTestItem> evaluated = itemRepository.findByMockTestIdOrderByItemOrderAsc(id);
		assertThat(evaluated.get(0).getEvaluationStatus()).isEqualTo(AnswerEvaluationStatus.DONE);
		assertThat(evaluated.get(0).getOverallLevel()).isEqualTo(OpicLevel.IM);
		assertThat(new ObjectMapper().readTree(evaluated.get(0).getEvaluation()).path("mode").asText()).isEqualTo("FULL");
		assertThat(evaluated.subList(13, 15)).allMatch(i -> i.getEvaluationStatus() == AnswerEvaluationStatus.SKIPPED);
	}

	@Test
	void historyListsOnlySubmittedTestsAndOthersCannotSeeThem() {
		UUID userId = userWithCredits(10);
		UUID abandoned = commandService.start(userId, survey(), 3, OpicLevel.IM).test().getId();
		UUID submitted = commandService.start(userId, survey(), 3, OpicLevel.IM).test().getId();
		commandService.submit(userId, submitted, List.of(), 60);
		UUID stranger = userWithCredits(10);

		assertThat(queryService.list(userId, 10)).extracting(s -> s.test().getId()).containsExactly(submitted);
		assertThat(queryService.list(userId, 10).get(0).itemCount()).isEqualTo(7);
		assertThatThrownBy(() -> queryService.get(stranger, submitted))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.MOCK_TEST_NOT_FOUND);
		assertThatThrownBy(() -> commandService.submit(stranger, abandoned, List.of(), 60))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.MOCK_TEST_NOT_FOUND);
	}

	@Test
	void deleteRemovesTheTestItsItemsAndItsJob() {
		UUID userId = userWithCredits(10);
		UUID id = commandService.start(userId, survey(), 3, OpicLevel.IM).test().getId();
		commandService.submit(userId, id, List.of(), 60);

		commandService.delete(userId, id);

		assertThat(testRepository.findById(id)).isEmpty();
		assertThat(itemRepository.findByMockTestIdOrderByItemOrderAsc(id)).isEmpty();
		assertThat(jobRepository.count()).isZero();
	}

	private static Map<String, List<String>> survey() {
		Map<String, List<String>> survey = new HashMap<>();
		survey.put("OCCUPATION", List.of("occupation.business"));
		survey.put("STUDENT", List.of("student.no"));
		survey.put("RESIDENCE", List.of("residence.family"));
		survey.put("LEISURE", List.of("leisure.movies", "leisure.park", "leisure.tv", "leisure.shopping", "leisure.cafe"));
		survey.put("HOBBY", List.of("hobby.music", "hobby.reading"));
		survey.put("SPORT", List.of("sport.jogging", "sport.walking", "sport.swimming"));
		survey.put("TRAVEL", List.of("travel.domestic", "travel.overseas"));
		return survey;
	}

	private void ensureTopic(String title, String category) {
		if (topicRepository.findByTitle(title).isEmpty()) {
			topicRepository.save(new Topic(title, title, category, 1, 1, List.of(), true));
		}
		if (questionRepository.findByActiveTrueAndTopicOrderByCreatedAtAscIdAsc(title).isEmpty()) {
			for (QuestionType type : List.of(QuestionType.DESCRIPTION, QuestionType.PAST_EXPERIENCE, QuestionType.COMPARE_CONTRAST)) {
				questionRepository.saveAndFlush(new Question(title, type, title + " " + type, null, null,
					List.of(OpicLevel.IM), List.of()));
			}
		}
	}

	private UUID userWithCredits(int credits) {
		User user = userRepository.save(new User(AuthProvider.KAKAO, "mock-" + UUID.randomUUID(), "m@example.com",
			"mock-user", UserRole.USER));
		creditCommand.ensureBalance(user.getId());
		creditCommand.addBalance(user.getId(), credits);
		return user.getId();
	}
}
