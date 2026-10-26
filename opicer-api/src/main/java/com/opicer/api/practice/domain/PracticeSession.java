package com.opicer.api.practice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * A completed topic practice. One per topic selection; answers live in {@link PracticeAnswer}.
 */
@Entity
@Table(
	name = "practice_session",
	uniqueConstraints = @UniqueConstraint(name = "uk_practice_session_topic_selection", columnNames = {"topic_selection_id"}),
	indexes = @Index(name = "idx_practice_session_user_completed", columnList = "user_id, completed_at")
)
public class PracticeSession {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "topic_selection_id", nullable = false)
	private UUID topicSelectionId;

	@Column(name = "topic_id", nullable = false)
	private UUID topicId;

	@Column(name = "topic_title", nullable = false)
	private String topicTitle;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "completed_at", nullable = false)
	private Instant completedAt;

	protected PracticeSession() {
	}

	public PracticeSession(UUID userId, UUID topicSelectionId, UUID topicId, String topicTitle, Instant startedAt) {
		this.userId = userId;
		this.topicSelectionId = topicSelectionId;
		this.topicId = topicId;
		this.topicTitle = topicTitle;
		this.startedAt = startedAt;
	}

	@PrePersist
	void onCreate() {
		this.completedAt = Instant.now();
		if (this.startedAt == null) {
			this.startedAt = this.completedAt;
		}
	}

	public UUID getId() { return id; }
	public UUID getUserId() { return userId; }
	public UUID getTopicSelectionId() { return topicSelectionId; }
	public UUID getTopicId() { return topicId; }
	public String getTopicTitle() { return topicTitle; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getCompletedAt() { return completedAt; }
}
