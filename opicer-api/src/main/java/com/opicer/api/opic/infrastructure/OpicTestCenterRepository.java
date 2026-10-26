package com.opicer.api.opic.infrastructure;

import com.opicer.api.opic.domain.OpicTestCenter;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpicTestCenterRepository extends JpaRepository<OpicTestCenter, UUID> {
	List<OpicTestCenter> findByActiveTrueOrderByRegionNameAscNameAsc();

	List<OpicTestCenter> findByActiveTrueAndRegionNameOrderByNameAsc(String regionName);

	boolean existsByIdAndActiveTrue(UUID id);

	List<OpicTestCenter> findAllByOrderByRegionNameAscNameAsc();

	boolean existsByExternalKey(String externalKey);

	boolean existsByExternalKeyAndIdNot(String externalKey, UUID id);
}
