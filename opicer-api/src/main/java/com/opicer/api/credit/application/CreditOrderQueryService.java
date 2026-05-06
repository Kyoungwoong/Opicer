package com.opicer.api.credit.application;

import com.opicer.api.credit.domain.CreditOrder;
import com.opicer.api.credit.infrastructure.CreditOrderRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditOrderQueryService {

	private static final Logger log = LoggerFactory.getLogger(CreditOrderQueryService.class);

	private final CreditOrderRepository creditOrderRepository;

	public CreditOrderQueryService(CreditOrderRepository creditOrderRepository) {
		this.creditOrderRepository = creditOrderRepository;
	}

	@Transactional(readOnly = true)
	public CreditOrder getOrder(UUID orderId) {
		log.debug("Fetching credit order. orderId={}", orderId);
		return creditOrderRepository.findById(orderId)
			.orElseThrow(() -> new IllegalArgumentException("Order not found"));
	}
}
