package com.opicer.api.evaluation.application;

import com.opicer.api.evaluation.domain.EvaluationGuideKind;

/** The active admin guide of one kind, as the evaluation prompt uses it. */
public record EvaluationGuideNote(EvaluationGuideKind kind, int version, String content) {}
