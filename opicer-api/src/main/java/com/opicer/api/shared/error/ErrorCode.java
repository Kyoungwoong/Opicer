package com.opicer.api.shared.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	QUESTION_NOT_FOUND("QUESTION_NOT_FOUND", HttpStatus.NOT_FOUND, "Question not found"),
	PROMPT_NOT_FOUND("PROMPT_NOT_FOUND", HttpStatus.NOT_FOUND, "Prompt not found"),
	SENTENCE_NOT_FOUND("SENTENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "DailySentence not found"),
	UNIVERSAL_SENTENCE_NOT_FOUND("UNIVERSAL_SENTENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Universal sentence not found"),
	DUPLICATE_DATE("DUPLICATE_DATE", HttpStatus.CONFLICT, "DailySentence already exists for date"),
	UNIVERSAL_SENTENCE_DAILY_NOT_READY("UNIVERSAL_SENTENCE_DAILY_NOT_READY", HttpStatus.CONFLICT,
		"Universal sentences not ready for daily set"),
	TOPIC_NOT_FOUND("TOPIC_NOT_FOUND", HttpStatus.NOT_FOUND, "Topic not found"),
	TOPIC_INACTIVE("TOPIC_INACTIVE", HttpStatus.CONFLICT, "Topic is inactive"),
	VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Validation failed"),
	BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "Bad request"),
	DATA_INTEGRITY_VIOLATION("DATA_INTEGRITY_VIOLATION", HttpStatus.CONFLICT, "Data integrity violation"),
	INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
	AI_NOT_CONFIGURED("AI_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE, "AI service is not configured"),
	AI_TRANSCRIPTION_FAILED("AI_TRANSCRIPTION_FAILED", HttpStatus.BAD_GATEWAY, "Audio transcription failed"),
	AI_ANALYSIS_FAILED("AI_ANALYSIS_FAILED", HttpStatus.BAD_GATEWAY, "AI analysis failed");

	private final String code;
	private final HttpStatus httpStatus;
	private final String defaultMessage;

	ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
		this.code = code;
		this.httpStatus = httpStatus;
		this.defaultMessage = defaultMessage;
	}

	public String getCode() {
		return code;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	public String getDefaultMessage() {
		return defaultMessage;
	}
}
