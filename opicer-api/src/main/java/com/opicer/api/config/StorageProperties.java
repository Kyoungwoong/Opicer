package com.opicer.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opicer.storage")
public class StorageProperties {

	private String goodAnswerDir = "./storage/good-answers";
	private String goodAnswerUrlPrefix = "/media/good-answers";

	public String getGoodAnswerDir() {
		return goodAnswerDir;
	}

	public void setGoodAnswerDir(String goodAnswerDir) {
		this.goodAnswerDir = goodAnswerDir;
	}

	public String getGoodAnswerUrlPrefix() {
		return goodAnswerUrlPrefix;
	}

	public void setGoodAnswerUrlPrefix(String goodAnswerUrlPrefix) {
		this.goodAnswerUrlPrefix = goodAnswerUrlPrefix;
	}
}
