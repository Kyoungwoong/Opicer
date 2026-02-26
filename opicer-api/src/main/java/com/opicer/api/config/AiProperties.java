package com.opicer.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opicer.ai")
public class AiProperties {

	private String openaiApiKey = "";
	private String anthropicApiKey = "";
	private String openaiEmbeddingModel = "text-embedding-3-small";
	private int openaiEmbeddingDimension = 1536;

	public String getOpenaiApiKey() {
		return openaiApiKey;
	}

	public void setOpenaiApiKey(String openaiApiKey) {
		this.openaiApiKey = openaiApiKey;
	}

	public String getAnthropicApiKey() {
		return anthropicApiKey;
	}

	public void setAnthropicApiKey(String anthropicApiKey) {
		this.anthropicApiKey = anthropicApiKey;
	}

	public String getOpenaiEmbeddingModel() {
		return openaiEmbeddingModel;
	}

	public void setOpenaiEmbeddingModel(String openaiEmbeddingModel) {
		this.openaiEmbeddingModel = openaiEmbeddingModel;
	}

	public int getOpenaiEmbeddingDimension() {
		return openaiEmbeddingDimension;
	}

	public void setOpenaiEmbeddingDimension(int openaiEmbeddingDimension) {
		this.openaiEmbeddingDimension = openaiEmbeddingDimension;
	}
}
