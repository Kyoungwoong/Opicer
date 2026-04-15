package com.opicer.api.credit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
	name = "credit_payments",
	uniqueConstraints = @UniqueConstraint(name = "uk_credit_payments_order", columnNames = {"order_id"})
)
public class CreditPayment {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(nullable = false)
	private String providerTxId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CreditPaymentStatus status;

	@Column(nullable = false)
	private Instant createdAt;

	protected CreditPayment() {
	}

	public CreditPayment(UUID orderId, String providerTxId, CreditPaymentStatus status) {
		this.orderId = orderId;
		this.providerTxId = providerTxId;
		this.status = status;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public UUID getId() { return id; }
	public UUID getOrderId() { return orderId; }
	public String getProviderTxId() { return providerTxId; }
	public CreditPaymentStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
}
