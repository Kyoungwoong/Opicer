package com.opicer.api.practice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
	name = "practice_credit_charge",
	uniqueConstraints = @UniqueConstraint(name = "uk_practice_credit_charge_topic_selection", columnNames = {"topic_selection_id"})
)
public class PracticeCreditCharge {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "topic_selection_id", nullable = false)
	private UUID topicSelectionId;

	@Column(nullable = false)
	private int amount;

	@Column(name = "charged_at", nullable = false)
	private Instant chargedAt;

	protected PracticeCreditCharge() {
	}

	public PracticeCreditCharge(UUID userId, UUID topicSelectionId, int amount) {
		this.userId = userId;
		this.topicSelectionId = topicSelectionId;
		this.amount = amount;
	}

	@PrePersist
	void onCreate() {
		this.chargedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public UUID getUserId() { return userId; }
	public UUID getTopicSelectionId() { return topicSelectionId; }
	public int getAmount() { return amount; }
	public Instant getChargedAt() { return chargedAt; }
}
