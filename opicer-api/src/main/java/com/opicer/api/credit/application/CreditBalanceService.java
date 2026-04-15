package com.opicer.api.credit.application;

import com.opicer.api.credit.domain.CreditBalance;
import com.opicer.api.credit.infrastructure.CreditBalanceRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditBalanceService {

	private static final Logger log = LoggerFactory.getLogger(CreditBalanceService.class);

	private final CreditBalanceRepository creditBalanceRepository;

	public CreditBalanceService(CreditBalanceRepository creditBalanceRepository) {
		this.creditBalanceRepository = creditBalanceRepository;
	}

	@Transactional
	public CreditBalance ensureBalance(UUID userId) {
		log.debug("Ensuring credit balance row exists. userId={}", userId);
		return creditBalanceRepository.findByUserId(userId)
			.orElseGet(() -> creditBalanceRepository.save(new CreditBalance(userId)));
	}

	@Transactional
	public void addBalance(UUID userId, long amount) {
		log.info("Increasing credit balance. userId={}, delta={}", userId, amount);
		int updated = creditBalanceRepository.addBalance(userId, amount);
		if (updated == 0) {
			log.warn("Balance row missing during increment; creating fallback row. userId={}", userId);
			CreditBalance created = creditBalanceRepository.save(new CreditBalance(userId));
			created.increase(amount);
			creditBalanceRepository.save(created);
		}
	}

	@Transactional(readOnly = true)
	public CreditBalance getBalance(UUID userId) {
		log.debug("Fetching credit balance. userId={}", userId);
		return creditBalanceRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("Balance not found"));
	}
}
