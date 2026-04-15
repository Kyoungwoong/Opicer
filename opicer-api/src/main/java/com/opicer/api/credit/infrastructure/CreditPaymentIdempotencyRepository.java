package com.opicer.api.credit.infrastructure;

import com.opicer.api.credit.domain.CreditPaymentIdempotency;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditPaymentIdempotencyRepository extends JpaRepository<CreditPaymentIdempotency, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from CreditPaymentIdempotency i where i.userId = :userId and i.idempotencyKey = :key")
	Optional<CreditPaymentIdempotency> findForUpdate(@Param("userId") UUID userId, @Param("key") String key);

	int deleteByExpiresAtBefore(Instant now);
}
