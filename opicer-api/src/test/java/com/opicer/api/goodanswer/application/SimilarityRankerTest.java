package com.opicer.api.goodanswer.application;

import com.opicer.api.goodanswer.application.SimilarityRanker.Candidate;
import com.opicer.api.goodanswer.application.SimilarityRanker.Ranked;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SimilarityRankerTest {

	private static final float[] QUERY = {1, 0, 0, 0};

	@Test
	void cosineHandlesUnnormalizedZeroAndMismatchedVectors() {
		assertThat(SimilarityRanker.cosine(new float[] {2, 0}, new float[] {5, 0})).isCloseTo(1.0, within(1e-9));
		assertThat(SimilarityRanker.cosine(new float[] {1, 0}, new float[] {0, 3})).isCloseTo(0.0, within(1e-9));
		assertThat(SimilarityRanker.cosine(new float[] {1, 1}, new float[] {-1, -1})).isCloseTo(-1.0, within(1e-9));
		assertThat(SimilarityRanker.cosine(new float[] {0, 0}, new float[] {1, 0})).isZero();
		assertThat(SimilarityRanker.cosine(new float[] {1, 0}, new float[] {1, 0, 0})).isZero();
		assertThat(SimilarityRanker.cosine(null, new float[] {1})).isZero();
	}

	@Test
	void dropsCandidatesBelowTheSimilarityFloor() {
		List<Ranked<String>> ranked = SimilarityRanker.select(QUERY, List.of(
			candidate("close", 1, 0.2f, 0, 0),
			candidate("orthogonal", 0, 1, 0, 0),
			candidate("opposite", -1, 0, 0, 0)
		), 3, 0.25, 0.7);

		assertThat(ranked).extracting(Ranked::item).containsExactly("close");
		assertThat(ranked.get(0).similarity()).isCloseTo(0.981, within(1e-3));
	}

	@Test
	void mmrPrefersADifferentSampleOverANearDuplicate() {
		// a1 and a2 are near-identical; b is slightly less relevant but points in a different direction.
		List<Candidate<String>> candidates = List.of(
			candidate("a1", 1, 0, 0.3f, 0),
			candidate("a2", 1, 0, 0.3f, 0.02f),
			candidate("b", 1, 0.35f, 0, 0)
		);

		List<Ranked<String>> diverse = SimilarityRanker.select(QUERY, candidates, 2, 0.25, 0.7);
		List<Ranked<String>> relevanceOnly = SimilarityRanker.select(QUERY, candidates, 2, 0.25, 1.0);

		assertThat(diverse).extracting(Ranked::item).containsExactly("a1", "b");
		assertThat(relevanceOnly).extracting(Ranked::item).containsExactly("a1", "a2");
	}

	@Test
	void tiesBreakOnIdSoResultsAreDeterministic() {
		List<Ranked<String>> ranked = SimilarityRanker.select(QUERY, List.of(
			candidate("b", 1, 0, 0, 0),
			candidate("a", 1, 0, 0, 0)
		), 1, 0.25, 0.7);

		assertThat(ranked).extracting(Ranked::item).containsExactly("a");
	}

	@Test
	void returnsFewerThanKWhenCandidatesRunOut() {
		assertThat(SimilarityRanker.select(QUERY, List.of(candidate("only", 1, 0, 0, 0)), 3, 0.25, 0.7))
			.hasSize(1);
		assertThat(SimilarityRanker.<String>select(QUERY, List.of(), 3, 0.25, 0.7)).isEmpty();
	}

	private static Candidate<String> candidate(String id, float... vector) {
		return new Candidate<>(id, vector, id);
	}
}
