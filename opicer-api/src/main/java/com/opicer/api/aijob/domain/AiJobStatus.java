package com.opicer.api.aijob.domain;

/** READY → RUNNING → SUCCEEDED, or back to READY for a retry, or FAILED once retries run out. */
public enum AiJobStatus {
	READY,
	RUNNING,
	SUCCEEDED,
	FAILED
}
