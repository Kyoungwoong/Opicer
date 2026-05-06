package com.opicer.api.credit.application;

import com.opicer.api.credit.domain.CreditOrder;
import com.opicer.api.credit.infrastructure.CreditOrderRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditOrderCommandService {

	private static final Logger log = LoggerFactory.getLogger(CreditOrderCommandService.class);

	private final CreditOrderRepository creditOrderRepository;
	private final CreditBalanceCommandService creditBalanceCommandService;

	public CreditOrderCommandService(
		CreditOrderRepository creditOrderRepository,
		CreditBalanceCommandService creditBalanceCommandService
	) {
		this.creditOrderRepository = creditOrderRepository;
		this.creditBalanceCommandService = creditBalanceCommandService;
	}

	@Transactional
	public CreditOrder createOrder(UUID userId, String packageId, int amount) {
		log.info("Creating credit order. userId={}, packageId={}, amount={}", userId, packageId, amount);
		creditBalanceCommandService.ensureBalance(userId);
		CreditOrder order = new CreditOrder(userId, packageId, amount);
		CreditOrder saved = creditOrderRepository.save(order);
		log.info("Credit order created. orderId={}, userId={}, status={}", saved.getId(), userId, saved.getStatus());
		return saved;
	}
}
