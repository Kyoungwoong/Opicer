package com.opicer.api.credit.infrastructure;

import com.opicer.api.credit.domain.CreditPayment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditPaymentRepository extends JpaRepository<CreditPayment, UUID> {
	Optional<CreditPayment> findByOrderId(UUID orderId);
	long countByOrderId(UUID orderId);
}
