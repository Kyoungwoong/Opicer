package com.opicer.api.credit.application;

import com.opicer.api.credit.domain.CreditOrder;
import com.opicer.api.credit.infrastructure.CreditPaymentRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CreditPaymentRetryTest {

	@Autowired
	private CreditOrderService creditOrderService;

	@Autowired
	private CreditPaymentService creditPaymentService;

	@Autowired
	private CreditPaymentRepository creditPaymentRepository;

	@Autowired
	private CreditBalanceService creditBalanceService;

	@Test
	void retryAfterTimeoutCanDuplicatePaymentAndBalance() {
		UUID userId = UUID.randomUUID();
		CreditOrder order = creditOrderService.createOrder(userId, "PACK_10", 10000);

		// First request succeeds but client perceives timeout (simulated at controller level in real flow).
		creditPaymentService.confirmPayment(order.getId(), "TX-RETRY-1");

		// Client retries the same logical request (no idempotency key in vulnerable version).
		creditPaymentService.confirmPayment(order.getId(), "TX-RETRY-2");

		long count = creditPaymentRepository.countByOrderId(order.getId());
		long balance = creditBalanceService.getBalance(order.getUserId()).getBalance();

		assertThat(count).isGreaterThan(1);
		assertThat(balance).isGreaterThan(order.getAmount());
	}
}
