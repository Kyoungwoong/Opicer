package com.opicer.api.credit.application;

import com.opicer.api.config.CreditProperties;
import com.opicer.api.credit.domain.CreditOrder;
import com.opicer.api.credit.domain.CreditPayment;
import com.opicer.api.credit.domain.CreditPaymentStatus;
import com.opicer.api.credit.domain.MockPaymentDecision;
import com.opicer.api.credit.domain.MockPaymentRecord;
import com.opicer.api.credit.infrastructure.CreditPaymentRepository;
import com.opicer.api.credit.infrastructure.MockPaymentRecordRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditPaymentService {

	private final CreditOrderService creditOrderService;
	private final CreditPaymentRepository creditPaymentRepository;
	private final MockPaymentRecordRepository mockPaymentRecordRepository;
	private final CreditProperties creditProperties;
	private final CreditBalanceService creditBalanceService;

	public CreditPaymentService(
		CreditOrderService creditOrderService,
		CreditPaymentRepository creditPaymentRepository,
		MockPaymentRecordRepository mockPaymentRecordRepository,
		CreditProperties creditProperties,
		CreditBalanceService creditBalanceService
	) {
		this.creditOrderService = creditOrderService;
		this.creditPaymentRepository = creditPaymentRepository;
		this.mockPaymentRecordRepository = mockPaymentRecordRepository;
		this.creditProperties = creditProperties;
		this.creditBalanceService = creditBalanceService;
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

	private void sleepIfNeeded() {
		long delay = creditProperties.getUnsafeDelayMs();
		if (delay <= 0) return;
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
