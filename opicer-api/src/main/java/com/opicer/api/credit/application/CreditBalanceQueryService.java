package com.opicer.api.credit.application;

import com.opicer.api.credit.domain.CreditBalance;
import com.opicer.api.credit.infrastructure.CreditBalanceRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditBalanceQueryService {

	private static final Logger log = LoggerFactory.getLogger(CreditBalanceQueryService.class);

	private final CreditBalanceRepository creditBalanceRepository;

	public CreditBalanceQueryService(CreditBalanceRepository creditBalanceRepository) {
		this.creditBalanceRepository = creditBalanceRepository;
	}

	@Transactional(readOnly = true)
	public CreditBalance getBalance(UUID userId) {
		log.debug("Fetching credit balance. userId={}", userId);
		return creditBalanceRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("Balance not found"));
	}

	@Transactional(readOnly = true)
	public boolean hasEnough(UUID userId, long amount) {
		return creditBalanceRepository.findByUserId(userId)
			.map(balance -> balance.getBalance() >= amount)
			.orElse(false);
	}
}
