package com.opicer.api.credit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.config.CreditProperties;
import com.opicer.api.credit.domain.CreditOrder;
import com.opicer.api.credit.domain.CreditPayment;
import com.opicer.api.credit.domain.CreditPaymentIdempotency;
import com.opicer.api.credit.domain.CreditPaymentStatus;
import com.opicer.api.credit.domain.MockPaymentDecision;
import com.opicer.api.credit.domain.MockPaymentRecord;
import com.opicer.api.credit.infrastructure.CreditPaymentIdempotencyRepository;
import com.opicer.api.credit.infrastructure.CreditPaymentRepository;
import com.opicer.api.credit.infrastructure.MockPaymentRecordRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditPaymentService {

	private final CreditOrderService creditOrderService;
	private final CreditPaymentRepository creditPaymentRepository;
	private final CreditPaymentIdempotencyRepository creditPaymentIdempotencyRepository;
	private final MockPaymentRecordRepository mockPaymentRecordRepository;
	private final CreditProperties creditProperties;
	private final CreditBalanceService creditBalanceService;
	private final ObjectMapper objectMapper;

	public CreditPaymentService(
		CreditOrderService creditOrderService,
		CreditPaymentRepository creditPaymentRepository,
		CreditPaymentIdempotencyRepository creditPaymentIdempotencyRepository,
		MockPaymentRecordRepository mockPaymentRecordRepository,
		CreditProperties creditProperties,
		CreditBalanceService creditBalanceService,
		ObjectMapper objectMapper
	) {
		this.creditOrderService = creditOrderService;
		this.creditPaymentRepository = creditPaymentRepository;
		this.creditPaymentIdempotencyRepository = creditPaymentIdempotencyRepository;
		this.mockPaymentRecordRepository = mockPaymentRecordRepository;
		this.creditProperties = creditProperties;
		this.creditBalanceService = creditBalanceService;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public CreditPayment confirmPayment(UUID orderId, String providerTxId) {
		CreditOrder order = creditOrderService.getOrder(orderId);
		mockPaymentRecordRepository.save(new MockPaymentRecord(providerTxId, MockPaymentDecision.APPROVED));

		// NOTE: INTENTIONALLY VULNERABLE
		// check-then-act without unique constraint or lock -> race condition allows duplicates.
		return creditPaymentRepository.findByOrderId(order.getId())
			.orElseGet(() -> {
				sleepIfNeeded();
				CreditPayment payment = new CreditPayment(order.getId(), providerTxId, CreditPaymentStatus.APPROVED);
				order.markPaid();
				// NOTE: INTENTIONALLY VULNERABLE
				// Balance update can be executed multiple times if duplicate payment is created.
				creditBalanceService.addBalance(order.getUserId(), order.getAmount());
				return creditPaymentRepository.save(payment);
			});
	}

	@Transactional
	public CreditPayment confirmPaymentWithIdempotency(UUID orderId, String providerTxId, String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "Idempotency-Key header is required");
		}
		CreditOrder order = creditOrderService.getOrder(orderId);
		String requestHash = computeRequestHash(orderId, providerTxId);
		Instant now = Instant.now();
		Instant expiresAt = now.plus(creditProperties.getIdempotencyTtlHours(), ChronoUnit.HOURS);
		CreditPaymentIdempotency idem = acquireIdempotency(order.getUserId(), idempotencyKey, requestHash, now, expiresAt);

		if (idem.isCompleted()) {
			return creditPaymentRepository.findById(idem.getPaymentId())
				.orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "Saved idempotency response is invalid"));
		}

		CreditPayment payment = confirmPayment(orderId, providerTxId);
		idem.complete(payment.getId(), HttpStatus.OK.value(), toSnapshot(payment));
		creditPaymentIdempotencyRepository.save(idem);
		return payment;
	}

	private CreditPaymentIdempotency acquireIdempotency(
		UUID userId,
		String idempotencyKey,
		String requestHash,
		Instant now,
		Instant expiresAt
	) {
		CreditPaymentIdempotency existing = creditPaymentIdempotencyRepository
			.findForUpdate(userId, idempotencyKey)
			.orElse(null);
		if (existing != null) {
			return validateOrReuse(existing, requestHash, now, expiresAt);
		}

		try {
			CreditPaymentIdempotency created = creditPaymentIdempotencyRepository
				.saveAndFlush(new CreditPaymentIdempotency(userId, idempotencyKey, requestHash, expiresAt));
			return created;
		} catch (DataIntegrityViolationException ex) {
			CreditPaymentIdempotency conflicted = creditPaymentIdempotencyRepository
				.findForUpdate(userId, idempotencyKey)
				.orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to read idempotency key"));
			return validateOrReuse(conflicted, requestHash, now, expiresAt);
		}
	}

	private CreditPaymentIdempotency validateOrReuse(
		CreditPaymentIdempotency existing,
		String requestHash,
		Instant now,
		Instant expiresAt
	) {
		if (existing.getExpiresAt().isBefore(now)) {
			existing.refreshForNewRequest(requestHash, expiresAt);
			return creditPaymentIdempotencyRepository.save(existing);
		}
		if (!existing.getRequestHash().equals(requestHash)) {
			throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT,
				"Idempotency-Key already used with different request payload");
		}
		return existing;
	}

	private String computeRequestHash(UUID orderId, String providerTxId) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String raw = orderId + "|" + providerTxId;
			byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed);
		} catch (NoSuchAlgorithmException ex) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "Unable to compute idempotency hash");
		}
	}

	private String toSnapshot(CreditPayment payment) {
		try {
			return objectMapper.writeValueAsString(new PaymentSnapshot(
				payment.getId(),
				payment.getOrderId(),
				payment.getProviderTxId(),
				payment.getStatus().name(),
				payment.getCreatedAt()
			));
		} catch (JsonProcessingException ex) {
			throw new ApiException(ErrorCode.INTERNAL_ERROR, "Unable to serialize idempotency response");
		}
	}

	private void sleepIfNeeded() {
		long delay = creditProperties.getUnsafeDelayMs();
		if (delay <= 0) return;
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private record PaymentSnapshot(
		UUID paymentId,
		UUID orderId,
		String providerTxId,
		String status,
		Instant createdAt
	) {
	}
}
