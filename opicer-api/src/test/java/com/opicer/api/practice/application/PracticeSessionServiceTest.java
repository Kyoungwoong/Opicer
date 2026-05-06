package com.opicer.api.practice.application;

import com.opicer.api.credit.application.CreditBalanceCommandService;
import com.opicer.api.credit.application.CreditBalanceQueryService;
import com.opicer.api.practice.domain.TopicSelection;
import com.opicer.api.practice.infrastructure.PracticeCreditChargeRepository;
import com.opicer.api.practice.infrastructure.TopicSelectionRepository;
import com.opicer.api.shared.error.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PracticeSessionCommandServiceTest {

	@Autowired
	private PracticeSessionCommandService practiceSessionCommandService;

	@Autowired
	private TopicSelectionRepository topicSelectionRepository;

	@Autowired
	private PracticeCreditChargeRepository practiceCreditChargeRepository;

	@Autowired
	private CreditBalanceCommandService creditBalanceService;

	@Autowired
	private CreditBalanceQueryService creditBalanceQueryService;

	@BeforeEach
	void setUp() {
		practiceCreditChargeRepository.deleteAll();
		topicSelectionRepository.deleteAll();
	}

	@Test
	void submitDeductsOnlyOnceForSameSelection() {
		UUID userId = UUID.randomUUID();
		creditBalanceService.ensureBalance(userId);
		creditBalanceService.addBalance(userId, 10);
		TopicSelection selection = topicSelectionRepository.save(new TopicSelection(userId, UUID.randomUUID()));

		PracticeSessionCommandService.SubmitResult first = practiceSessionCommandService.submit(userId, selection.getId());
		PracticeSessionCommandService.SubmitResult second = practiceSessionCommandService.submit(userId, selection.getId());

		assertThat(first.alreadyCharged()).isFalse();
		assertThat(second.alreadyCharged()).isTrue();
		assertThat(practiceCreditChargeRepository.countByTopicSelectionId(selection.getId())).isEqualTo(1);
		assertThat(creditBalanceQueryService.getBalance(userId).getBalance()).isEqualTo(8);
	}

	@Test
	void submitConcurrentRequestsDeductsOnce() throws Exception {
		UUID userId = UUID.randomUUID();
		creditBalanceService.ensureBalance(userId);
		creditBalanceService.addBalance(userId, 10);
		TopicSelection selection = topicSelectionRepository.save(new TopicSelection(userId, UUID.randomUUID()));

		ExecutorService pool = Executors.newFixedThreadPool(10);
		CountDownLatch ready = new CountDownLatch(10);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> futures = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			futures.add(pool.submit(() -> {
				ready.countDown();
				start.await();
				practiceSessionCommandService.submit(userId, selection.getId());
				return true;
			}));
		}

		ready.await();
		start.countDown();
		for (Future<Boolean> future : futures) {
			assertThat(future.get()).isTrue();
		}
		pool.shutdown();

		assertThat(practiceCreditChargeRepository.countByTopicSelectionId(selection.getId())).isEqualTo(1);
		assertThat(creditBalanceQueryService.getBalance(userId).getBalance()).isEqualTo(8);
	}

	@Test
	void submitHighLoadConcurrentRequestsDeductsOnce() throws Exception {
		UUID userId = UUID.randomUUID();
		creditBalanceService.ensureBalance(userId);
		creditBalanceService.addBalance(userId, 100);
		TopicSelection selection = topicSelectionRepository.save(new TopicSelection(userId, UUID.randomUUID()));

		int threadPool = 10;
		int totalRequests = 500;
		ExecutorService pool = Executors.newFixedThreadPool(threadPool);
		CountDownLatch ready = new CountDownLatch(totalRequests);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger();

		List<Future<Boolean>> futures = new ArrayList<>();
		for (int i = 0; i < totalRequests; i++) {
			futures.add(pool.submit(() -> {
				ready.countDown();
				start.await();
				practiceSessionCommandService.submit(userId, selection.getId());
				successCount.incrementAndGet();
				return true;
			}));
		}

		ready.await();
		start.countDown();
		for (Future<Boolean> future : futures) {
			assertThat(future.get()).isTrue();
		}
		pool.shutdown();

		assertThat(successCount.get()).isEqualTo(totalRequests);
		assertThat(practiceCreditChargeRepository.countByTopicSelectionId(selection.getId())).isEqualTo(1);
		assertThat(creditBalanceQueryService.getBalance(userId).getBalance()).isEqualTo(98);
	}

	@Test
	void submitConcurrentDifferentSelectionsWithExactBalance_doesNotGoNegative() throws Exception {
		UUID userId = UUID.randomUUID();
		creditBalanceService.ensureBalance(userId);
		creditBalanceService.addBalance(userId, 2);

		TopicSelection first = topicSelectionRepository.save(new TopicSelection(userId, UUID.randomUUID()));
		TopicSelection second = topicSelectionRepository.save(new TopicSelection(userId, UUID.randomUUID()));

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> futures = List.of(
			pool.submit(callSubmit(userId, first.getId(), ready, start)),
			pool.submit(callSubmit(userId, second.getId(), ready, start))
		);
		ready.await();
		start.countDown();

		int success = 0;
		int failed = 0;
		for (Future<Boolean> future : futures) {
			if (future.get()) success++;
			else failed++;
		}
		pool.shutdown();

		assertThat(success).isEqualTo(1);
		assertThat(failed).isEqualTo(1);
		assertThat(creditBalanceQueryService.getBalance(userId).getBalance()).isZero();
		assertThat(practiceCreditChargeRepository.count()).isEqualTo(1);
	}

	@Test
	void submitWithUnknownSelectionOwnedByOtherUser_returnsNotFound() {
		UUID owner = UUID.randomUUID();
		UUID attacker = UUID.randomUUID();
		creditBalanceService.ensureBalance(owner);
		creditBalanceService.addBalance(owner, 10);
		creditBalanceService.ensureBalance(attacker);
		creditBalanceService.addBalance(attacker, 10);
		TopicSelection selection = topicSelectionRepository.save(new TopicSelection(owner, UUID.randomUUID()));

		assertThatThrownBy(() -> practiceSessionCommandService.submit(attacker, selection.getId()))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining("Topic selection not found");
	}

	@Test
	void submitFailsWhenBalanceIsInsufficient() {
		UUID userId = UUID.randomUUID();
		creditBalanceService.ensureBalance(userId);
		creditBalanceService.addBalance(userId, 1);
		TopicSelection selection = topicSelectionRepository.save(new TopicSelection(userId, UUID.randomUUID()));

		assertThatThrownBy(() -> practiceSessionCommandService.submit(userId, selection.getId()))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining("At least 2 credits");
		assertThat(practiceCreditChargeRepository.countByTopicSelectionId(selection.getId())).isZero();
		assertThat(creditBalanceQueryService.getBalance(userId).getBalance()).isEqualTo(1);
	}

	private Callable<Boolean> callSubmit(
		UUID userId,
		UUID selectionId,
		CountDownLatch ready,
		CountDownLatch start
	) {
		return () -> {
			ready.countDown();
			start.await();
			try {
				practiceSessionCommandService.submit(userId, selectionId);
				return true;
			} catch (ApiException ex) {
				return false;
			}
		};
	}
}
