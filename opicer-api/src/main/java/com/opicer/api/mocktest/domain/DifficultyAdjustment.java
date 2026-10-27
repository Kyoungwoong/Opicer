package com.opicer.api.mocktest.domain;

/** The mid-test re-selection: how the second part's question difficulty moves. */
public enum DifficultyAdjustment {
	EASIER(-1),
	SAME(0),
	HARDER(1);

	private final int shift;

	DifficultyAdjustment(int shift) {
		this.shift = shift;
	}

	public int shift() {
		return shift;
	}
}
