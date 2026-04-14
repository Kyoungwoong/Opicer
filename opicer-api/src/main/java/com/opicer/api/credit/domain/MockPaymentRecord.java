package com.opicer.api.credit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mock_payment_provider")
public class MockPaymentRecord {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false)
	private String providerTxId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MockPaymentDecision decision;

	@Column(nullable = false)
	private Instant createdAt;

	protected MockPaymentRecord() {
	}

	public MockPaymentRecord(String providerTxId, MockPaymentDecision decision) {
		this.providerTxId = providerTxId;
		this.decision = decision;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public UUID getId() { return id; }
	public String getProviderTxId() { return providerTxId; }
	public MockPaymentDecision getDecision() { return decision; }
	public Instant getCreatedAt() { return createdAt; }
}
