package com.opicer.api.credit.presentation;

import com.opicer.api.credit.application.CreditOrderService;
import com.opicer.api.credit.application.CreditPaymentService;
import com.opicer.api.credit.domain.CreditOrder;
import com.opicer.api.credit.domain.CreditPayment;
import com.opicer.api.auth.domain.AuthUserPrincipal;
import com.opicer.api.shared.presentation.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credits")
public class CreditPurchaseController {

	private final CreditOrderService creditOrderService;
	private final CreditPaymentService creditPaymentService;

	public CreditPurchaseController(
		CreditOrderService creditOrderService,
		CreditPaymentService creditPaymentService
	) {
		this.creditOrderService = creditOrderService;
		this.creditPaymentService = creditPaymentService;
	}

	@PostMapping("/orders")
	public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
		Authentication authentication,
		@Valid @RequestBody OrderRequest request
	) {
		UUID currentUserId = resolveCurrentUserId(authentication);
		CreditOrder order = creditOrderService.createOrder(currentUserId, request.packageId(), request.amount());
		return ResponseEntity.status(201).body(ApiResponse.created("CREDIT_ORDER_CREATED",
			new OrderResponse(order.getId(), order.getUserId(), order.getPackageId(), order.getAmount(), order.getStatus().name(),
				order.getCreatedAt())));
	}

	@PostMapping("/payments/confirm")
	public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
		@RequestHeader("Idempotency-Key") String idempotencyKey,
		@Valid @RequestBody PaymentConfirmRequest request
	) {
		CreditPayment payment = creditPaymentService.confirmPaymentWithIdempotency(
			request.orderId(),
			request.providerTxId(),
			idempotencyKey
		);
		if (request.simulateTimeout()) {
			throw new org.springframework.web.server.ResponseStatusException(
				org.springframework.http.HttpStatus.GATEWAY_TIMEOUT,
				"Simulated timeout after payment approval"
			);
		}
		return ResponseEntity.ok(ApiResponse.ok("CREDIT_PAYMENT_APPROVED",
			new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getProviderTxId(), payment.getStatus().name(),
				payment.getCreatedAt())));
	}

	public record OrderRequest(@NotBlank String packageId, @Positive int amount) {}

	public record OrderResponse(
		UUID orderId,
		UUID userId,
		String packageId,
		int amount,
		String status,
		Instant createdAt
	) {}

	public record PaymentConfirmRequest(
		@NotNull UUID orderId,
		@NotBlank String providerTxId,
		boolean simulateTimeout
	) {}

	public record PaymentResponse(
		UUID paymentId,
		UUID orderId,
		String providerTxId,
		String status,
		Instant createdAt
	) {}

	private UUID resolveCurrentUserId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
			throw new org.springframework.web.server.ResponseStatusException(
				org.springframework.http.HttpStatus.UNAUTHORIZED,
				"Authentication is required"
			);
		}
		return principal.id();
	}
}
