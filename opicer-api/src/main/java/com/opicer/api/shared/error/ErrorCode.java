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
	TOPIC_TITLE_DUPLICATE("TOPIC_TITLE_DUPLICATE", HttpStatus.CONFLICT, "A topic with this title already exists"),
	TOPIC_TITLE_LOCKED("TOPIC_TITLE_LOCKED", HttpStatus.CONFLICT, "The mock test refers to this topic by its title"),
	TOPIC_IN_USE("TOPIC_IN_USE", HttpStatus.CONFLICT, "The topic is used by questions or practice history"),
	VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Validation failed"),
	BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "Bad request"),
	UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Sign-in required"),
	NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND, "No such endpoint"),
	METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed for this endpoint"),
	DATA_INTEGRITY_VIOLATION("DATA_INTEGRITY_VIOLATION", HttpStatus.CONFLICT, "Data integrity violation"),
	INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
	AI_NOT_CONFIGURED("AI_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE, "AI service is not configured"),
	AI_TRANSCRIPTION_FAILED("AI_TRANSCRIPTION_FAILED", HttpStatus.BAD_GATEWAY, "Audio transcription failed"),
	AI_EMBEDDING_FAILED("AI_EMBEDDING_FAILED", HttpStatus.BAD_GATEWAY, "Embedding generation failed"),
	AI_RAG_FAILED("AI_RAG_FAILED", HttpStatus.BAD_GATEWAY, "RAG retrieval failed"),
	IDEMPOTENCY_KEY_CONFLICT("IDEMPOTENCY_KEY_CONFLICT", HttpStatus.CONFLICT, "Idempotency key already used with different request"),
	TOPIC_SELECTION_NOT_FOUND("TOPIC_SELECTION_NOT_FOUND", HttpStatus.NOT_FOUND, "Topic selection not found"),
	PRACTICE_SESSION_NOT_FOUND("PRACTICE_SESSION_NOT_FOUND", HttpStatus.NOT_FOUND, "Practice session not found"),
	PRACTICE_ANSWER_NOT_FOUND("PRACTICE_ANSWER_NOT_FOUND", HttpStatus.NOT_FOUND, "Practice answer not found"),
	MOCK_TEST_NOT_FOUND("MOCK_TEST_NOT_FOUND", HttpStatus.NOT_FOUND, "Mock test not found"),
	MOCK_TEST_NOT_IN_PROGRESS("MOCK_TEST_NOT_IN_PROGRESS", HttpStatus.CONFLICT, "This mock test was already submitted"),
	MOCK_TEST_ALREADY_ADJUSTED("MOCK_TEST_ALREADY_ADJUSTED", HttpStatus.CONFLICT, "The difficulty was already re-selected"),
	MOCK_TEST_NO_QUESTIONS("MOCK_TEST_NO_QUESTIONS", HttpStatus.SERVICE_UNAVAILABLE, "The question bank has no questions for a mock test"),
	SURVEY_ITEM_NOT_FOUND("SURVEY_ITEM_NOT_FOUND", HttpStatus.NOT_FOUND, "Survey item not found"),
	OPIC_CENTER_NOT_FOUND("OPIC_CENTER_NOT_FOUND", HttpStatus.NOT_FOUND, "OPIc test center not found"),
	OPIC_CENTER_DUPLICATE("OPIC_CENTER_DUPLICATE", HttpStatus.CONFLICT, "A test center with this name and address already exists"),
	OPIC_CENTER_REVIEW_NOT_FOUND("OPIC_CENTER_REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND, "OPIc test center review not found"),
	OPIC_SCHEDULE_NOT_FOUND("OPIC_SCHEDULE_NOT_FOUND", HttpStatus.NOT_FOUND, "OPIc exam schedule not found"),
	OPIC_SCHEDULE_DUPLICATE("OPIC_SCHEDULE_DUPLICATE", HttpStatus.CONFLICT,
		"An exam schedule with this date and round name already exists"),
	EVALUATION_NOT_RETRYABLE("EVALUATION_NOT_RETRYABLE", HttpStatus.CONFLICT, "Evaluation can only be retried after it failed"),
	EVALUATION_RETRY_LIMIT("EVALUATION_RETRY_LIMIT", HttpStatus.CONFLICT, "No evaluation retries left for this session"),
	EVALUATION_GUIDE_NOT_FOUND("EVALUATION_GUIDE_NOT_FOUND", HttpStatus.NOT_FOUND, "Evaluation guide not found"),
	EVALUATION_GUIDE_ACTIVE("EVALUATION_GUIDE_ACTIVE", HttpStatus.CONFLICT,
		"An active guide cannot be edited or deleted; create a new version instead"),
	CREDIT_INSUFFICIENT_BALANCE("CREDIT_INSUFFICIENT_BALANCE", HttpStatus.PAYMENT_REQUIRED, "Insufficient credits");

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
