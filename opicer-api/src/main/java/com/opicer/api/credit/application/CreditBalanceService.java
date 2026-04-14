package com.opicer.api.credit.application;

import com.opicer.api.credit.domain.CreditBalance;
import com.opicer.api.credit.infrastructure.CreditBalanceRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditBalanceService {

	private final CreditBalanceRepository creditBalanceRepository;

	public CreditBalanceService(CreditBalanceRepository creditBalanceRepository) {
		this.creditBalanceRepository = creditBalanceRepository;
	}

	@Transactional
	public CreditBalance ensureBalance(UUID userId) {
		return creditBalanceRepository.findByUserId(userId)
			.orElseGet(() -> creditBalanceRepository.save(new CreditBalance(userId)));
	}

	@Transactional
	public void addBalance(UUID userId, long amount) {
		int updated = creditBalanceRepository.addBalance(userId, amount);
		if (updated == 0) {
			CreditBalance created = creditBalanceRepository.save(new CreditBalance(userId));
			created.increase(amount);
			creditBalanceRepository.save(created);
		}
	}

	@Transactional(readOnly = true)
	public CreditBalance getBalance(UUID userId) {
		return creditBalanceRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("Balance not found"));
	}
}
