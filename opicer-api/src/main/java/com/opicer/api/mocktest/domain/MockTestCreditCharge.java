package com.opicer.api.mocktest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/** The one charge for a submitted mock test; the unique test id makes a resubmission free. */
@Entity
@Table(
	name = "mock_test_credit_charge",
	uniqueConstraints = @UniqueConstraint(name = "uk_mock_test_credit_charge_test", columnNames = {"mock_test_id"})
)
public class MockTestCreditCharge {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "mock_test_id", nullable = false)
	private UUID mockTestId;

	@Column(nullable = false)
	private int amount;

	@Column(name = "charged_at", nullable = false)
	private Instant chargedAt;

	protected MockTestCreditCharge() {
	}

	public MockTestCreditCharge(UUID userId, UUID mockTestId, int amount) {
		this.userId = userId;
		this.mockTestId = mockTestId;
		this.amount = amount;
	}

	@PrePersist
	void onCreate() {
		this.chargedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public UUID getUserId() { return userId; }
	public UUID getMockTestId() { return mockTestId; }
	public int getAmount() { return amount; }
	public Instant getChargedAt() { return chargedAt; }
}
