package com.opicer.api.evaluation.application;

/**
 * Why an answer could not be evaluated. {@code retryable} tells the worker whether another attempt can help
 * (rate limits, timeouts, server errors, truncated or invalid output) or not (refusal, bad configuration).
 */
public class EvaluationFailure extends RuntimeException {

	private final String reason;
	private final boolean retryable;

	public EvaluationFailure(String reason, boolean retryable, String detail) {
		super(reason + (detail == null || detail.isBlank() ? "" : ": " + detail));
		this.reason = reason;
		this.retryable = retryable;
	}

	public static EvaluationFailure retryable(String reason, String detail) {
		return new EvaluationFailure(reason, true, detail);
	}

	public static EvaluationFailure permanent(String reason, String detail) {
		return new EvaluationFailure(reason, false, detail);
	}

	/** Stable code such as {@code REFUSED}, {@code RATE_LIMITED}, {@code INVALID_OUTPUT}. */
	public String reason() {
		return reason;
	}

	public boolean retryable() {
		return retryable;
	}
}
