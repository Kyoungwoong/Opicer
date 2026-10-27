package com.opicer.api.evaluation.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.evaluation.application.AnswerEvaluator.AnswerToEvaluate;
import com.opicer.api.evaluation.domain.CompetencyDimension;
import com.opicer.api.evaluation.domain.EvaluationMode;
import com.opicer.api.goodanswer.application.GoodAnswerSampleQueryService;
import com.opicer.api.goodanswer.application.ReferenceSample;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static com.opicer.api.evaluation.application.EvaluationFixtures.TRANSCRIPT;
import static com.opicer.api.evaluation.application.EvaluationFixtures.validOutput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnswerEvaluatorTest {

	private final UUID topicId = UUID.randomUUID();
	private final GoodAnswerSampleQueryService references = mock(GoodAnswerSampleQueryService.class);
	private final List<EvaluationPrompt> prompts = new ArrayList<>();
	private EvaluationOutput nextOutput = validOutput();

	private final AnswerEvaluationModel model = new AnswerEvaluationModel() {
		@Override
		public EvaluationOutput evaluate(EvaluationPrompt prompt) {
			prompts.add(prompt);
			return nextOutput;
		}

		@Override
		public String provider() {
			return "fake";
		}

		@Override
		public String model() {
			return "fake-model";
		}
	};

	private final AnswerEvaluator evaluator = new AnswerEvaluator(model, references);

	@Test
	void coachingStoresNoLevelRatingsOrAssessment() throws Exception {
		AnswerEvaluationResult result = evaluator.evaluate(answer(TRANSCRIPT), List.of(), EvaluationMode.COACH);

		assertThat(result.skipped()).isFalse();
		assertThat(result.overallLevel()).isNull();
		assertThat(result.assessment()).isNull();
		assertThat(result.summaryKo()).doesNotContain("예상 레벨").startsWith("고칠 점");
		assertThat(prompts.get(0).mode()).isEqualTo(EvaluationMode.COACH);
		JsonNode doc = new ObjectMapper().readTree(result.evaluationJson());
		assertThat(doc.path("mode").asText()).isEqualTo("COACH");
		assertThat(doc.path("meta").path("mode").asText()).isEqualTo("COACH");
		assertThat(doc.has("overallLevel")).isFalse();
		assertThat(doc.has("dimensions")).isFalse();
		assertThat(doc.has("strengths")).isFalse();
		assertThat(doc.path("corrections").size()).isEqualTo(3);
		assertThat(doc.path("targetScript").asText()).startsWith("Last summer, I went");
	}

	@Test
	void evaluatesAnAnswerIntoAStoredDocumentLevelSummaryAndAssessment() throws Exception {
		ReferenceSample sample = new ReferenceSample(UUID.randomUUID(), OpicLevel.IH, null, List.of(), "A sample.", 0.81234);
		when(references.findReferenceSamples(topicId, TRANSCRIPT)).thenReturn(List.of(sample));

		AnswerEvaluationResult result = evaluator.evaluate(answer(TRANSCRIPT));

		assertThat(result.skipped()).isFalse();
		// ratings IM, IM, IL, IM → lower median IM; 41 words is under the 60-word cap, which allows IM.
		assertThat(result.overallLevel()).isEqualTo(OpicLevel.IM);
		assertThat(result.targetScript()).startsWith("Last summer, I went");
		assertThat(result.summaryKo()).contains("예상 레벨: IM").contains("공식 OPIc 점수가 아닙니다").contains("I go to Jeju → I went to Jeju");
		assertThat(result.assessment().dimensionLevels()).containsEntry(CompetencyDimension.ACCURACY, OpicLevel.IL);
		assertThat(result.assessment().corrections()).hasSize(3);

		JsonNode doc = new ObjectMapper().readTree(result.evaluationJson());
		assertThat(doc.path("schemaVersion").asInt()).isEqualTo(1);
		assertThat(doc.path("overallLevel").asText()).isEqualTo("IM");
		assertThat(doc.path("signals").path("wordCount").asInt()).isEqualTo(result.signals().wordCount());
		assertThat(doc.path("dimensions").size()).isEqualTo(4);
		assertThat(doc.path("strengths").size()).isEqualTo(2);
		assertThat(doc.path("meta").path("provider").asText()).isEqualTo("fake");
		assertThat(doc.path("meta").path("model").asText()).isEqualTo("fake-model");
		assertThat(doc.path("meta").path("promptVersion").asText()).isEqualTo(EvaluationPromptBuilder.PROMPT_VERSION);
		assertThat(doc.path("meta").path("referenceSampleIds").get(0).asText()).isEqualTo(sample.id().toString());
		assertThat(doc.path("meta").path("referenceSimilarities").get(0).asDouble()).isEqualTo(0.812);

		assertThat(prompts).hasSize(1);
		assertThat(prompts.get(0).input()).contains("Target level: IH").contains("<reference level=\"IH\">A sample.");
	}

	@Test
	void shortAnswersAreSkippedWithoutCallingTheModel() {
		AnswerEvaluationResult result = evaluator.evaluate(answer("I like my home."));

		assertThat(result.skipped()).isTrue();
		assertThat(result.evaluationJson()).isNull();
		assertThat(result.signals().wordCount()).isEqualTo(4);
		assertThat(prompts).isEmpty();
	}

	@Test
	void continuesWithoutReferencesWhenRetrievalFails() {
		when(references.findReferenceSamples(any(), any())).thenThrow(new ApiException(ErrorCode.AI_NOT_CONFIGURED));

		AnswerEvaluationResult result = evaluator.evaluate(answer(TRANSCRIPT));

		assertThat(result.skipped()).isFalse();
		assertThat(prompts.get(0).input()).contains("Reference samples: none");
	}

	@Test
	void invalidModelOutputSurfacesAsARetryableFailure() {
		when(references.findReferenceSamples(any(), any())).thenReturn(List.of());
		EvaluationOutput valid = validOutput();
		nextOutput = new EvaluationOutput(valid.dimensions().subList(0, 2), valid.strengths(), valid.corrections(),
			valid.targetScript(), valid.keyExpressions());

		assertThatThrownBy(() -> evaluator.evaluate(answer(TRANSCRIPT)))
			.isInstanceOfSatisfying(EvaluationFailure.class, e -> assertThat(e.retryable()).isTrue());
	}

	private AnswerToEvaluate answer(String transcript) {
		return new AnswerToEvaluate(topicId, "Tell me about a memorable trip.", "PAST_EXPERIENCE", null, List.of(),
			transcript, 45, OpicLevel.IH);
	}
}
