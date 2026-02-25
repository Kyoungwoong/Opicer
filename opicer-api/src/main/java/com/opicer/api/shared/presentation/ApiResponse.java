package com.opicer.api.shared.presentation;

public record ApiResponse<T>(int status, String message, T data) {

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(200, message, data);
	}

	public static <T> ApiResponse<T> created(String message, T data) {
		return new ApiResponse<>(201, message, data);
	}

	public static <T> ApiResponse<T> of(int status, String message, T data) {
		return new ApiResponse<>(status, message, data);
	}
}
