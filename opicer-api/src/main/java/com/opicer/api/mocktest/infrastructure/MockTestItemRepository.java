package com.opicer.api.mocktest.infrastructure;

import com.opicer.api.mocktest.domain.MockTestItem;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MockTestItemRepository extends JpaRepository<MockTestItem, UUID> {

	List<MockTestItem> findByMockTestIdOrderByItemOrderAsc(UUID mockTestId);

	List<MockTestItem> findByMockTestIdIn(Collection<UUID> mockTestIds);

	@Modifying
	@Query("delete from MockTestItem i where i.mockTestId = :mockTestId")
	int deleteByMockTestId(@Param("mockTestId") UUID mockTestId);
}
