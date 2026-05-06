package com.opicer.api.practice.application;

import com.opicer.api.credit.application.CreditBalanceService;
import com.opicer.api.practice.domain.TopicSelection;
import com.opicer.api.practice.infrastructure.PracticeCreditChargeRepository;
import com.opicer.api.practice.infrastructure.TopicSelectionRepository;
import com.opicer.api.shared.error.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PracticeSessionServiceTest {

	@Autowired
	private PracticeSessionService practiceSessionService;

	@Autowired
	private TopicSelectionRepository topicSelectionRepository;

	@Autowired
	private PracticeCreditChargeRepository practiceCreditChargeRepository;

	@Autowired
	private CreditBalanceService creditBalanceService;

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

		PracticeSessionService.SubmitResult first = practiceSessionService.submit(userId, selection.getId());
		PracticeSessionService.SubmitResult second = practiceSessionService.submit(userId, selection.getId());

		assertThat(first.alreadyCharged()).isFalse();
		assertThat(second.alreadyCharged()).isTrue();
		assertThat(practiceCreditChargeRepository.countByTopicSelectionId(selection.getId())).isEqualTo(1);
		assertThat(creditBalanceService.getBalance(userId).getBalance()).isEqualTo(8);
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
				practiceSessionService.submit(userId, selection.getId());
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
		assertThat(creditBalanceService.getBalance(userId).getBalance()).isEqualTo(8);
	}

	@Test
	void submitFailsWhenBalanceIsInsufficient() {
		UUID userId = UUID.randomUUID();
		creditBalanceService.ensureBalance(userId);
		creditBalanceService.addBalance(userId, 1);
		TopicSelection selection = topicSelectionRepository.save(new TopicSelection(userId, UUID.randomUUID()));

		assertThatThrownBy(() -> practiceSessionService.submit(userId, selection.getId()))
			.isInstanceOf(ApiException.class)
			.hasMessageContaining("At least 2 credits");
		assertThat(practiceCreditChargeRepository.countByTopicSelectionId(selection.getId())).isZero();
		assertThat(creditBalanceService.getBalance(userId).getBalance()).isEqualTo(1);
	}
}
