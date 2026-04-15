package com.opicer.api.credit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
	name = "credit_payment_idempotency",
	uniqueConstraints = @UniqueConstraint(name = "uk_credit_payment_idempotency_user_key", columnNames = {"userId", "idempotencyKey"})
)
public class CreditPaymentIdempotency {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false)
	private UUID userId;

	@Column(nullable = false, length = 128)
	private String idempotencyKey;

	@Column(nullable = false, length = 64)
	private String requestHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CreditPaymentIdempotencyStatus status;

	@Column
	private UUID paymentId;

	@Column
	private Integer responseStatus;

	@Lob
	@Column
	private String responseBody;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected CreditPaymentIdempotency() {
	}

	public CreditPaymentIdempotency(UUID userId, String idempotencyKey, String requestHash, Instant expiresAt) {
		this.userId = userId;
		this.idempotencyKey = idempotencyKey;
		this.requestHash = requestHash;
		this.status = CreditPaymentIdempotencyStatus.IN_PROGRESS;
		this.expiresAt = expiresAt;
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

	public UUID getId() { return id; }
	public UUID getUserId() { return userId; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public String getRequestHash() { return requestHash; }
	public CreditPaymentIdempotencyStatus getStatus() { return status; }
	public UUID getPaymentId() { return paymentId; }
	public Integer getResponseStatus() { return responseStatus; }
	public String getResponseBody() { return responseBody; }
	public Instant getExpiresAt() { return expiresAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }

	public boolean isCompleted() {
		return this.status == CreditPaymentIdempotencyStatus.COMPLETED;
	}

	public void complete(UUID paymentId, int responseStatus, String responseBody) {
		this.paymentId = paymentId;
		this.responseStatus = responseStatus;
		this.responseBody = responseBody;
		this.status = CreditPaymentIdempotencyStatus.COMPLETED;
	}

	public void refreshForNewRequest(String requestHash, Instant expiresAt) {
		this.requestHash = requestHash;
		this.status = CreditPaymentIdempotencyStatus.IN_PROGRESS;
		this.paymentId = null;
		this.responseStatus = null;
		this.responseBody = null;
		this.expiresAt = expiresAt;
	}
}
