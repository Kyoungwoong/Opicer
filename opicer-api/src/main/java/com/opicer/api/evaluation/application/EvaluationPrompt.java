package com.opicer.api.evaluation.application;

/**
 * @param instructions stable system text (rubric, rules); identical across calls so providers can cache it
 * @param input the per-answer part (question, signals, references, transcript)
 */
public record EvaluationPrompt(String instructions, String input) {}
