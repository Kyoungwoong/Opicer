package com.opicer.api.opic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * An admin-entered review of an {@link OpicTestCenter}. Centers are deactivated rather than deleted on sync,
 * so {@code centerId} stays valid; hidden reviews ({@code visible = false}) are admin-only.
 */
@Entity
@Table(name = "opic_center_review", indexes = @Index(name = "idx_opic_center_review_center", columnList = "center_id"))
public class OpicCenterReview {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "center_id", nullable = false)
	private UUID centerId;

	/** 1..5 */
	@Column(nullable = false)
	private int rating;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	/** Where the review came from, e.g. "응시자 제보". Optional. */
	@Column(name = "source_label", length = 100)
	private String sourceLabel;

	@Column(nullable = false)
	private boolean visible = true;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected OpicCenterReview() {
	}

	public OpicCenterReview(UUID centerId, int rating, String content, String sourceLabel, boolean visible) {
		this.centerId = centerId;
		update(rating, content, sourceLabel, visible);
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public void update(int rating, String content, String sourceLabel, boolean visible) {
		this.rating = rating;
		this.content = content;
		this.sourceLabel = sourceLabel;
		this.visible = visible;
	}

	public UUID getId() { return id; }
	public UUID getCenterId() { return centerId; }
	public int getRating() { return rating; }
	public String getContent() { return content; }
	public String getSourceLabel() { return sourceLabel; }
	public boolean isVisible() { return visible; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
