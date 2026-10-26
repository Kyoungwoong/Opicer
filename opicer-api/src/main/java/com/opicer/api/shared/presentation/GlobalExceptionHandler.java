package com.opicer.api.shared.presentation;

import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
		ErrorCode errorCode = ex.getErrorCode();
		ErrorResponse response = ErrorResponse.from(errorCode, ex.getMessage());
		return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
			.toList();
		ErrorResponse response = ErrorResponse.from(ErrorCode.VALIDATION_ERROR,
			ErrorCode.VALIDATION_ERROR.getDefaultMessage(), errors);
		return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus()).body(response);
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<ErrorResponse> handleBindException(BindException ex) {
		List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
			.toList();
		ErrorResponse response = ErrorResponse.from(ErrorCode.VALIDATION_ERROR,
			ErrorCode.VALIDATION_ERROR.getDefaultMessage(), errors);
		return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus()).body(response);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		ErrorResponse response = ErrorResponse.from(ErrorCode.BAD_REQUEST, ex.getMessage());
		return ResponseEntity.status(ErrorCode.BAD_REQUEST.getHttpStatus()).body(response);
	}

	@ExceptionHandler({
		MissingServletRequestParameterException.class,
		MissingServletRequestPartException.class,
		MissingRequestHeaderException.class
	})
	public ResponseEntity<ErrorResponse> handleMissingParam(Exception ex) {
		ErrorResponse response = ErrorResponse.from(ErrorCode.VALIDATION_ERROR,
			ErrorCode.VALIDATION_ERROR.getDefaultMessage());
		return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus()).body(response);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
		ErrorResponse response = ErrorResponse.from(ErrorCode.BAD_REQUEST, "Malformed JSON request");
		return ResponseEntity.status(ErrorCode.BAD_REQUEST.getHttpStatus()).body(response);
	}

	@ExceptionHandler({DataIntegrityViolationException.class, ConstraintViolationException.class})
	public ResponseEntity<ErrorResponse> handleDataIntegrity(Exception ex) {
		ErrorResponse response = ErrorResponse.from(ErrorCode.DATA_INTEGRITY_VIOLATION,
			ErrorCode.DATA_INTEGRITY_VIOLATION.getDefaultMessage());
		return ResponseEntity.status(ErrorCode.DATA_INTEGRITY_VIOLATION.getHttpStatus()).body(response);
	}

	/** An unmapped path (including removed endpoints) is a 404, not a server error. */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
		ErrorResponse response = ErrorResponse.from(ErrorCode.NOT_FOUND);
		return ResponseEntity.status(ErrorCode.NOT_FOUND.getHttpStatus()).body(response);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		ErrorResponse response = ErrorResponse.from(ErrorCode.METHOD_NOT_ALLOWED);
		return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus()).body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception ex) {
		ErrorResponse response = ErrorResponse.from(ErrorCode.INTERNAL_ERROR);
		return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus()).body(response);
	}
}
