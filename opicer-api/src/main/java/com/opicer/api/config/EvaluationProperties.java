package com.opicer.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Answer-evaluation model settings. {@code provider} chooses the {@code AnswerEvaluationModel} implementation. */
@ConfigurationProperties(prefix = "opicer.ai.evaluation")
public class EvaluationProperties {

	public static final String PROVIDER_KEY = "opicer.ai.evaluation.provider";

	/** {@code openai} (default). */
	private String provider = "openai";
	private String openaiModel = "gpt-5.6-sol";
	private String openaiUrl = "https://api.openai.com/v1/responses";
	/** Reasoning effort sent to the model, e.g. low / medium / high. */
	private String reasoningEffort = "medium";
	/** Caps visible output plus reasoning tokens together. */
	private int maxOutputTokens = 16000;
	private int timeoutSeconds = 90;

	public String getProvider() { return provider; }
	public void setProvider(String provider) { this.provider = provider; }
	public String getOpenaiModel() { return openaiModel; }
	public void setOpenaiModel(String openaiModel) { this.openaiModel = openaiModel; }
	public String getOpenaiUrl() { return openaiUrl; }
	public void setOpenaiUrl(String openaiUrl) { this.openaiUrl = openaiUrl; }
	public String getReasoningEffort() { return reasoningEffort; }
	public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }
	public int getMaxOutputTokens() { return maxOutputTokens; }
	public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
	public int getTimeoutSeconds() { return timeoutSeconds; }
	public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
