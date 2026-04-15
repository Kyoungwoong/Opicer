package com.opicer.api.credit.application;

import com.opicer.api.credit.domain.CreditOrder;
import com.opicer.api.credit.infrastructure.CreditPaymentRepository;
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

@SpringBootTest
@TestPropertySource(properties = "opicer.credit.unsafe-delay-ms=50")
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
	void retryDuringInFlightCanDuplicatePaymentAndBalance() throws Exception {
		UUID userId = UUID.randomUUID();
		CreditOrder order = creditOrderService.createOrder(userId, "PACK_10", 10000);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch doneGate = new CountDownLatch(2);

		executor.submit(() -> {
			try {
				startGate.await();
				creditPaymentService.confirmPayment(order.getId(), "TX-RETRY-1");
			} catch (Exception ignored) {
			} finally {
				doneGate.countDown();
			}
		});
		executor.submit(() -> {
			try {
				startGate.await();
				creditPaymentService.confirmPayment(order.getId(), "TX-RETRY-2");
			} catch (Exception ignored) {
			} finally {
				doneGate.countDown();
			}
		});

		startGate.countDown();
		assertThat(doneGate.await(10, TimeUnit.SECONDS)).isTrue();
		executor.shutdown();

		long count = creditPaymentRepository.countByOrderId(order.getId());
		long balance = creditBalanceService.getBalance(order.getUserId()).getBalance();

		assertThat(count).isGreaterThan(1);
		assertThat(balance).isGreaterThan(order.getAmount());
	}
}
