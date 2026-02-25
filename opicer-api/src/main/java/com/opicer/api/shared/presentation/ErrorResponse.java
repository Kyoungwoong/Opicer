package com.opicer.api.shared.presentation;

import com.opicer.api.shared.error.ErrorCode;
import java.util.List;

public record ErrorResponse(
	int status,
	String code,
	String message,
	List<FieldError> errors
) {

	public static ErrorResponse from(ErrorCode errorCode, String message, List<FieldError> errors) {
		return new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getCode(), message, errors);
	}

	public static ErrorResponse from(ErrorCode errorCode, String message) {
		return from(errorCode, message, null);
	}

	public static ErrorResponse from(ErrorCode errorCode) {
		return from(errorCode, errorCode.getDefaultMessage(), null);
	}

	public record FieldError(String field, String message) {}
}
