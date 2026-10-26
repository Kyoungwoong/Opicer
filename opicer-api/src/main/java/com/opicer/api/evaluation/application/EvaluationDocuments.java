package com.opicer.api.evaluation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opicer.api.evaluation.domain.AnswerAssessment;
import com.opicer.api.evaluation.domain.AnswerAssessment.CorrectionSample;
import com.opicer.api.evaluation.domain.CompetencyDimension;
import com.opicer.api.evaluation.domain.CorrectionCategory;
import com.opicer.api.evaluation.domain.SessionReport;
import com.opicer.api.shared.domain.OpicLevel;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the stored JSON documents: the per-answer evaluation ({@code schemaVersion} 1, written by
 * {@link AnswerEvaluator}) and the session report.
 */
public final class EvaluationDocuments {

	static final int REPORT_SCHEMA_VERSION = 1;
	private static final ObjectMapper JSON = new ObjectMapper();

	private EvaluationDocuments() {
	}

	/** Rebuilds the aggregator input from a stored answer evaluation, so earlier attempts' results count too. */
	public static AnswerAssessment assessment(String evaluationJson) {
		JsonNode doc = read(evaluationJson);
		Map<CompetencyDimension, OpicLevel> levels = new EnumMap<>(CompetencyDimension.class);
		for (JsonNode dimension : doc.path("dimensions")) {
			levels.put(CompetencyDimension.valueOf(dimension.path("code").asText()),
				OpicLevel.valueOf(dimension.path("level").asText()));
		}
		List<CorrectionSample> corrections = new ArrayList<>();
		for (JsonNode correction : doc.path("corrections")) {
			corrections.add(new CorrectionSample(CorrectionCategory.valueOf(correction.path("category").asText()),
				correction.path("original").asText(), correction.path("corrected").asText()));
		}
		return new AnswerAssessment(levels, corrections, doc.path("signals").path("wordCount").asInt());
	}

	/** Report JSON: the computed {@link SessionReport} plus answer counts and the fixed disclaimer. */
	public static String report(SessionReport report, int skippedAnswers, int failedAnswers) {
		ObjectNode doc = JSON.valueToTree(report);
		doc.put("schemaVersion", REPORT_SCHEMA_VERSION);
		doc.put("skippedAnswers", skippedAnswers);
		doc.put("failedAnswers", failedAnswers);
		doc.put("disclaimer", SessionReport.DISCLAIMER);
		try {
			return JSON.writeValueAsString(doc);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("session report is not serialisable", e);
		}
	}

	private static JsonNode read(String json) {
		try {
			return JSON.readTree(json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("stored evaluation is not valid JSON", e);
		}
	}
}
