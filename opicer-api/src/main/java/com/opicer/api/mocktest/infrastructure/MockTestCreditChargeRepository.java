package com.opicer.api.mocktest.infrastructure;

import com.opicer.api.mocktest.domain.MockTestCreditCharge;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockTestCreditChargeRepository extends JpaRepository<MockTestCreditCharge, UUID> {

	Optional<MockTestCreditCharge> findByMockTestId(UUID mockTestId);
}
