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
class CreditPaymentServiceConcurrencyTest {

	@Autowired
	private CreditOrderService creditOrderService;

	@Autowired
	private CreditPaymentService creditPaymentService;

	@Autowired
	private CreditPaymentRepository creditPaymentRepository;

	@Autowired
	private com.opicer.api.credit.application.CreditBalanceService creditBalanceService;

	@Test
	void concurrentConfirmCreatesDuplicates() throws Exception {
		UUID userId = UUID.randomUUID();
		CreditOrder order = creditOrderService.createOrder(userId, "PACK_10", 10000);

		int threads = 10;
		int requestsPerThread = 10_000;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch doneGate = new CountDownLatch(threads);

		for (int i = 0; i < threads; i++) {
			executor.submit(() -> {
				try {
					startGate.await();
					for (int j = 0; j < requestsPerThread; j++) {
						String txId = "TX-" + Thread.currentThread().getName() + "-" + j;
						creditPaymentService.confirmPayment(order.getId(), txId);
					}
				} catch (Exception ignored) {
				} finally {
					doneGate.countDown();
				}
			});
		}

		startGate.countDown();
		doneGate.await(5, TimeUnit.SECONDS);
		executor.shutdown();

		long count = creditPaymentRepository.countByOrderId(order.getId());
		assertThat(count).isGreaterThan(1);

		long balance = creditBalanceService.getBalance(order.getUserId()).getBalance();
		assertThat(balance).isGreaterThan(order.getAmount());
	}
}
