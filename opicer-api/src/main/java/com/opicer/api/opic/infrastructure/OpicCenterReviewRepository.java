package com.opicer.api.opic.infrastructure;

import com.opicer.api.opic.domain.OpicCenterReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OpicCenterReviewRepository extends JpaRepository<OpicCenterReview, UUID> {
	List<OpicCenterReview> findByCenterIdOrderByCreatedAtDesc(UUID centerId);

	List<OpicCenterReview> findByCenterIdAndVisibleTrueOrderByCreatedAtDesc(UUID centerId);

	@Query("""
		select r.centerId as centerId, count(r) as reviewCount, avg(r.rating) as averageRating
		from OpicCenterReview r
		where r.visible = true
		group by r.centerId
		""")
	List<ReviewStats> findVisibleStats();

	interface ReviewStats {
		UUID getCenterId();

		long getReviewCount();

		Double getAverageRating();
	}
}
