package com.opicer.api.credit.application;

import com.opicer.api.credit.domain.CreditOrder;
import com.opicer.api.credit.domain.CreditPayment;
import com.opicer.api.credit.infrastructure.CreditPaymentRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "opicer.credit.unsafe-delay-ms=50")
class CreditPaymentIdempotencyTest {

	@Autowired
	private CreditOrderService creditOrderService;

	@Autowired
	private CreditPaymentService creditPaymentService;

	@Autowired
	private CreditPaymentRepository creditPaymentRepository;

	@Autowired
	private CreditBalanceService creditBalanceService;

	@Test
	void sameKeySamePayloadReturnsExistingPayment() {
		UUID userId = UUID.randomUUID();
		CreditOrder order = creditOrderService.createOrder(userId, "PACK_10", 10000);

		CreditPayment first = creditPaymentService
			.confirmPaymentWithIdempotency(order.getId(), "TX-IDEMPOTENT-1", "idem-key-1");
		CreditPayment second = creditPaymentService
			.confirmPaymentWithIdempotency(order.getId(), "TX-IDEMPOTENT-1", "idem-key-1");

		assertThat(second.getId()).isEqualTo(first.getId());
		assertThat(creditPaymentRepository.countByOrderId(order.getId())).isEqualTo(1);
		assertThat(creditBalanceService.getBalance(order.getUserId()).getBalance()).isEqualTo(order.getAmount());
	}

	@Test
	void sameKeyDifferentPayloadThrowsConflict() {
		UUID userId = UUID.randomUUID();
		CreditOrder order = creditOrderService.createOrder(userId, "PACK_10", 10000);

		creditPaymentService.confirmPaymentWithIdempotency(order.getId(), "TX-FIRST", "idem-key-2");

		assertThatThrownBy(() -> creditPaymentService
			.confirmPaymentWithIdempotency(order.getId(), "TX-SECOND", "idem-key-2"))
			.isInstanceOf(ApiException.class)
			.satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT));
	}

	@Test
	void concurrentSameKeyCreatesSinglePaymentAndBalanceUpdate() throws Exception {
		UUID userId = UUID.randomUUID();
		CreditOrder order = creditOrderService.createOrder(userId, "PACK_10", 10000);
		int threads = 20;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch doneGate = new CountDownLatch(threads);

		for (int i = 0; i < threads; i++) {
			executor.submit(() -> {
				try {
					startGate.await();
					creditPaymentService.confirmPaymentWithIdempotency(order.getId(), "TX-SAME", "idem-key-3");
				} catch (Exception ignored) {
				} finally {
					doneGate.countDown();
				}
			});
		}

		startGate.countDown();
		assertThat(doneGate.await(10, TimeUnit.SECONDS)).isTrue();
		executor.shutdown();

		assertThat(creditPaymentRepository.countByOrderId(order.getId())).isEqualTo(1);
		assertThat(creditBalanceService.getBalance(order.getUserId()).getBalance()).isEqualTo(order.getAmount());
	}
}
