package com.opicer.api.goodanswer.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Second retrieval stage: drops weak matches and picks a diverse top-k with Maximal Marginal Relevance.
 * Pure and deterministic (ties break on id) so ranking can be unit-tested without a database.
 */
public final class SimilarityRanker {

	private SimilarityRanker() {
	}

	/** A candidate vector; {@code id} only breaks ties. */
	public record Candidate<T>(String id, float[] vector, T item) {}

	public record Ranked<T>(T item, double similarity) {}

	/** Cosine similarity in [-1, 1]; 0 when either vector is empty or zero. */
	public static double cosine(float[] a, float[] b) {
		if (a == null || b == null || a.length == 0 || a.length != b.length) {
			return 0;
		}
		double dot = 0;
		double normA = 0;
		double normB = 0;
		for (int i = 0; i < a.length; i++) {
			dot += (double) a[i] * b[i];
			normA += (double) a[i] * a[i];
			normB += (double) b[i] * b[i];
		}
		if (normA == 0 || normB == 0) {
			return 0;
		}
		return dot / (Math.sqrt(normA) * Math.sqrt(normB));
	}

	/**
	 * Repeatedly picks {@code argmax λ·sim(q,c) − (1−λ)·max_{s∈S} sim(c,s)} among candidates whose
	 * similarity to the query is at least {@code minSimilarity}, until {@code k} are chosen.
	 */
	public static <T> List<Ranked<T>> select(
		float[] query,
		List<Candidate<T>> candidates,
		int k,
		double minSimilarity,
		double lambda
	) {
		List<Scored<T>> pool = new ArrayList<>();
		for (Candidate<T> candidate : candidates) {
			double similarity = cosine(query, candidate.vector());
			if (similarity >= minSimilarity) {
				pool.add(new Scored<>(candidate, similarity));
			}
		}
		// Stable starting order makes tie-breaking deterministic.
		pool.sort(Comparator.comparingDouble((Scored<T> s) -> -s.similarity).thenComparing(s -> s.candidate.id()));

		List<Scored<T>> selected = new ArrayList<>();
		while (selected.size() < k && !pool.isEmpty()) {
			Scored<T> best = null;
			double bestScore = Double.NEGATIVE_INFINITY;
			for (Scored<T> scored : pool) {
				double redundancy = 0;
				for (Scored<T> chosen : selected) {
					redundancy = Math.max(redundancy, cosine(scored.candidate.vector(), chosen.candidate.vector()));
				}
				double score = lambda * scored.similarity - (1 - lambda) * redundancy;
				if (score > bestScore) {
					bestScore = score;
					best = scored;
				}
			}
			selected.add(best);
			pool.remove(best);
		}
		return selected.stream().map(s -> new Ranked<>(s.candidate.item(), s.similarity)).toList();
	}

	private record Scored<T>(Candidate<T> candidate, double similarity) {}
}
