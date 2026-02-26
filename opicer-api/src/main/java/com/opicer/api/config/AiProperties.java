package com.opicer.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opicer.ai")
public class AiProperties {

	private String openaiApiKey = "";
	private String anthropicApiKey = "";

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
}
