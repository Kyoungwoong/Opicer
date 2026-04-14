package com.opicer.api.credit.infrastructure;

import com.opicer.api.credit.domain.CreditBalance;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditBalanceRepository extends JpaRepository<CreditBalance, UUID> {
	Optional<CreditBalance> findByUserId(UUID userId);

	@Modifying
	@Query("update CreditBalance b set b.balance = b.balance + :delta where b.userId = :userId")
	int addBalance(@Param("userId") UUID userId, @Param("delta") long delta);
}
