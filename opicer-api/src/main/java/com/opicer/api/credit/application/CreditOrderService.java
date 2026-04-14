package com.opicer.api.credit.application;

import com.opicer.api.credit.domain.CreditOrder;
import com.opicer.api.credit.infrastructure.CreditOrderRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditOrderService {

	private final CreditOrderRepository creditOrderRepository;
	private final CreditBalanceService creditBalanceService;

	public CreditOrderService(CreditOrderRepository creditOrderRepository, CreditBalanceService creditBalanceService) {
		this.creditOrderRepository = creditOrderRepository;
		this.creditBalanceService = creditBalanceService;
	}

	@Transactional
	public CreditOrder createOrder(UUID userId, String packageId, int amount) {
		creditBalanceService.ensureBalance(userId);
		CreditOrder order = new CreditOrder(userId, packageId, amount);
		return creditOrderRepository.save(order);
	}

	@Transactional(readOnly = true)
	public CreditOrder getOrder(UUID orderId) {
		return creditOrderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("Order not found"));
	}
}
