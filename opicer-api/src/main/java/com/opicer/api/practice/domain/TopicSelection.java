package com.opicer.api.practice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topic_selection")
public class TopicSelection {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private UUID topicId;

	@Column(nullable = false)
	private Instant selectedAt;

	protected TopicSelection() {
	}

	public TopicSelection(UUID userId, UUID topicId) {
		this.userId = userId;
		this.topicId = topicId;
	}

	@PrePersist
	void onCreate() {
		this.selectedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public UUID getUserId() { return userId; }
	public UUID getTopicId() { return topicId; }
	public Instant getSelectedAt() { return selectedAt; }
}
