package com.opicer.api.credit.infrastructure;

import com.opicer.api.credit.domain.MockPaymentRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockPaymentRecordRepository extends JpaRepository<MockPaymentRecord, UUID> {
}
