package com.opicer.api.practice.domain;

import jakarta.persistence.Column;
import com.opicer.api.shared.domain.OpicLevel;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

	// Evaluation columns are nullable: sessions recorded before automatic evaluation have none.
	@Enumerated(EnumType.STRING)
	@Column(name = "target_level", length = 2)
	private OpicLevel targetLevel;

	@Enumerated(EnumType.STRING)
	@Column(name = "evaluation_status", length = 16)
	private SessionEvaluationStatus evaluationStatus;

	/** Estimate for practice, never an official OPIc score. */
	@Enumerated(EnumType.STRING)
	@Column(name = "estimated_level", length = 2)
	private OpicLevel estimatedLevel;

	/** Session report as JSON (see evaluation.domain.SessionReport). */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private String report;

	@Column(name = "evaluated_at")
	private Instant evaluatedAt;

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

	/** Marks the session as waiting for automatic evaluation against {@code targetLevel}. */
	public void requestEvaluation(OpicLevel targetLevel) {
		this.targetLevel = targetLevel;
		this.evaluationStatus = SessionEvaluationStatus.PENDING;
	}

	public UUID getId() { return id; }
	public UUID getUserId() { return userId; }
	public UUID getTopicSelectionId() { return topicSelectionId; }
	public UUID getTopicId() { return topicId; }
	public String getTopicTitle() { return topicTitle; }
	public Instant getStartedAt() { return startedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public OpicLevel getTargetLevel() { return targetLevel; }
	public SessionEvaluationStatus getEvaluationStatus() { return evaluationStatus; }
	public OpicLevel getEstimatedLevel() { return estimatedLevel; }
	public String getReport() { return report; }
	public Instant getEvaluatedAt() { return evaluatedAt; }
}
