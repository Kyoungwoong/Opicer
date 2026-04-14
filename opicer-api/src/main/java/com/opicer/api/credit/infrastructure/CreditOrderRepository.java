package com.opicer.api.credit.infrastructure;

import com.opicer.api.credit.domain.CreditOrder;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditOrderRepository extends JpaRepository<CreditOrder, UUID> {
}
