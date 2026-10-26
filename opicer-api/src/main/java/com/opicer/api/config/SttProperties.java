package com.opicer.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Speech-to-text settings. {@code provider} chooses the {@code SpeechToText} implementation. */
@ConfigurationProperties(prefix = "opicer.ai.stt")
public class SttProperties {

	public static final String PROVIDER_KEY = "opicer.ai.stt.provider";

	/** {@code mock} (default, no network) or {@code openai}. */
	private String provider = "mock";
	private String openaiModel = "whisper-1";
	private String openaiUrl = "https://api.openai.com/v1/audio/transcriptions";

	public String getProvider() { return provider; }
	public void setProvider(String provider) { this.provider = provider; }
	public String getOpenaiModel() { return openaiModel; }
	public void setOpenaiModel(String openaiModel) { this.openaiModel = openaiModel; }
	public String getOpenaiUrl() { return openaiUrl; }
	public void setOpenaiUrl(String openaiUrl) { this.openaiUrl = openaiUrl; }
}
