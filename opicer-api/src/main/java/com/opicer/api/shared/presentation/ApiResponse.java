package com.opicer.api.shared.presentation;

import org.springframework.http.HttpStatus;

public record ApiResponse<T>(int status, String message, T data) {

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(HttpStatus.OK.value(), message, data);
	}

	public static <T> ApiResponse<T> created(String message, T data) {
		return new ApiResponse<>(HttpStatus.CREATED.value(), message, data);
	}

	public static <T> ApiResponse<T> of(HttpStatus status, String message, T data) {
		return new ApiResponse<>(status.value(), message, data);
	}
}
