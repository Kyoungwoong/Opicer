package com.opicer.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reference-sample retrieval tuning. See docs/adr/0001-vector-similarity.md for why these defaults.
 */
@ConfigurationProperties(prefix = "opicer.rag")
public class RagProperties {

	/** Nearest neighbours fetched by cosine distance before re-ranking. */
	private int candidates = 12;
	/** Samples finally handed to the prompt. */
	private int topK = 3;
	/** Cosine similarity floor; candidates below it are dropped. Calibrate from logs. */
	private double minSimilarity = 0.25;
	/** MMR trade-off: 1.0 = pure relevance, 0.0 = pure diversity. */
	private double mmrLambda = 0.7;
	/** Transcripts shorter than this (in words) are too thin to retrieve against. */
	private int minQueryWords = 15;

	public int getCandidates() { return candidates; }
	public void setCandidates(int candidates) { this.candidates = candidates; }
	public int getTopK() { return topK; }
	public void setTopK(int topK) { this.topK = topK; }
	public double getMinSimilarity() { return minSimilarity; }
	public void setMinSimilarity(double minSimilarity) { this.minSimilarity = minSimilarity; }
	public double getMmrLambda() { return mmrLambda; }
	public void setMmrLambda(double mmrLambda) { this.mmrLambda = mmrLambda; }
	public int getMinQueryWords() { return minQueryWords; }
	public void setMinQueryWords(int minQueryWords) { this.minQueryWords = minQueryWords; }
}
