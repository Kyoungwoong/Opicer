package com.opicer.api.mocktest.domain;

/** A test is kept in history only once submitted; an abandoned one stays IN_PROGRESS and is never listed. */
public enum MockTestStatus {
	IN_PROGRESS,
	SUBMITTED
}
