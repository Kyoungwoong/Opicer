package com.opicer.api.evaluation.domain;

/**
 * Heuristic length caps: a very short answer cannot show paragraph-level speech, however clean it is.
 * Below {@code ilMaxWords} words the answer is capped at IL, below {@code imMaxWords} at IM.
 */
public record LevelPolicy(int ilMaxWords, int imMaxWords) {

	public static final LevelPolicy DEFAULT = new LevelPolicy(30, 60);

	public LevelPolicy {
		if (ilMaxWords < 0 || imMaxWords < ilMaxWords) {
			throw new IllegalArgumentException("need 0 <= ilMaxWords <= imMaxWords");
		}
	}
}
