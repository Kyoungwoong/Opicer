package com.opicer.api.topic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TopicBadge {

	@Column(nullable = false)
	private String label;

	@Column
	private Integer count;

	protected TopicBadge() {
	}

	public TopicBadge(String label, Integer count) {
		this.label = label;
		this.count = count;
	}

	public String getLabel() {
		return label;
	}

	public Integer getCount() {
		return count;
	}
}
