package com.opicer.api.evaluation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.evaluation.application.EvaluationOutput.DimensionRating;
import com.opicer.api.evaluation.domain.AnswerAssessment;
import com.opicer.api.evaluation.domain.AnswerAssessment.CorrectionSample;
import com.opicer.api.evaluation.domain.CompetencyDimension;
import com.opicer.api.evaluation.domain.LevelAggregator;
import com.opicer.api.evaluation.domain.LevelPolicy;
import com.opicer.api.evaluation.domain.SpeechSignals;
import com.opicer.api.evaluation.domain.SpeechSignalsCalculator;
import com.opicer.api.goodanswer.application.GoodAnswerSampleQueryService;
import com.opicer.api.goodanswer.application.ReferenceSample;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.error.ApiException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Evaluates one answer end to end — signals → reference samples → model → validation → stored document —
 * without touching the database, so the worker can call it outside a transaction and persist the result itself.
 */
@Service
public class AnswerEvaluator {

	private static final Logger log = LoggerFactory.getLogger(AnswerEvaluator.class);

	/** Shorter answers cannot show enough to rate four competencies. */
	static final int MIN_WORDS = 15;
	static final int SCHEMA_VERSION = 1;

	private final AnswerEvaluationModel model;
	private final GoodAnswerSampleQueryService referenceSamples;
	private final LevelAggregator aggregator = new LevelAggregator(LevelPolicy.DEFAULT);
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AnswerEvaluator(AnswerEvaluationModel model, GoodAnswerSampleQueryService referenceSamples) {
		this.model = model;
		this.referenceSamples = referenceSamples;
	}

	/**
	 * @throws EvaluationFailure when the model declines, errors, or returns unusable output
	 */
	public AnswerEvaluationResult evaluate(AnswerToEvaluate answer) {
		return evaluate(answer, List.of());
	}

	/**
	 * @param guides active admin guides to add to the instructions; may be empty
	 * @throws EvaluationFailure when the model declines, errors, or returns unusable output
	 */
	public AnswerEvaluationResult evaluate(AnswerToEvaluate answer, List<EvaluationGuideNote> guides) {
		SpeechSignals signals = SpeechSignalsCalculator.calculate(answer.transcript(), answer.durationSeconds());
		if (signals.wordCount() < MIN_WORDS) {
			return AnswerEvaluationResult.skipped(signals);
		}
		List<ReferenceSample> references = references(answer.topicId(), answer.transcript());
		EvaluationInput input = new EvaluationInput(answer.questionText(), answer.questionType(),
			answer.structuralHint(), answer.keyExpressions(), answer.transcript(), answer.targetLevel(), signals, references);

		EvaluationOutput evaluation = EvaluationValidator.validate(
			model.evaluate(EvaluationPromptBuilder.build(input, guides)), answer.transcript());

		AnswerAssessment assessment = assessment(evaluation, signals);
		OpicLevel overall = aggregator.answerLevel(assessment);
		return new AnswerEvaluationResult(false, signals, document(evaluation, overall, signals, references, guides), overall,
			EvaluationSummaryRenderer.render(evaluation, overall), evaluation.targetScript(), assessment);
	}

	/** Reference samples only enrich the prompt; a retrieval failure means "none". */
	private List<ReferenceSample> references(UUID topicId, String transcript) {
		try {
			return referenceSamples.findReferenceSamples(topicId, transcript);
		} catch (ApiException e) {
			log.warn("[Eval] reference retrieval skipped: {}", e.getErrorCode());
			return List.of();
		}
	}

	private static AnswerAssessment assessment(EvaluationOutput evaluation, SpeechSignals signals) {
		Map<CompetencyDimension, OpicLevel> levels = new EnumMap<>(CompetencyDimension.class);
		for (DimensionRating rating : evaluation.dimensions()) {
			levels.put(rating.code(), rating.level());
		}
		List<CorrectionSample> corrections = evaluation.corrections().stream()
			.map(c -> new CorrectionSample(c.category(), c.original(), c.corrected()))
			.toList();
		return new AnswerAssessment(levels, corrections, signals.wordCount());
	}

	private String document(EvaluationOutput evaluation, OpicLevel overall, SpeechSignals signals,
		List<ReferenceSample> references, List<EvaluationGuideNote> guides) {
		Map<String, Object> meta = new LinkedHashMap<>();
		meta.put("provider", model.provider());
		meta.put("model", model.model());
		meta.put("promptVersion", EvaluationPromptBuilder.PROMPT_VERSION);
		meta.put("referenceSampleIds", references.stream().map(ReferenceSample::id).toList());
		meta.put("referenceSimilarities", references.stream().map(r -> Math.round(r.similarity() * 1000) / 1000.0).toList());
		meta.put("guides", guides.stream().map(g -> new GuideUsed(g.kind().name(), g.version())).toList());

		Map<String, Object> doc = new LinkedHashMap<>();
		doc.put("schemaVersion", SCHEMA_VERSION);
		doc.put("overallLevel", overall);
		doc.put("signals", signals);
		doc.put("dimensions", evaluation.dimensions());
		doc.put("strengths", evaluation.strengths());
		doc.put("corrections", evaluation.corrections());
		doc.put("targetScript", evaluation.targetScript());
		doc.put("keyExpressions", evaluation.keyExpressions());
		doc.put("meta", meta);
		try {
			return objectMapper.writeValueAsString(doc);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("evaluation document is not serialisable", e);
		}
	}

	/** Which guide version shaped an evaluation, kept in its meta for tracing. */
	private record GuideUsed(String kind, int version) {}

	/**
	 * One stored answer plus the question metadata the prompt uses.
	 *
	 * @param durationSeconds recording length; null when unknown
	 */
	public record AnswerToEvaluate(
		UUID topicId,
		String questionText,
		String questionType,
		String structuralHint,
		List<String> keyExpressions,
		String transcript,
		Integer durationSeconds,
		OpicLevel targetLevel
	) {}
}
