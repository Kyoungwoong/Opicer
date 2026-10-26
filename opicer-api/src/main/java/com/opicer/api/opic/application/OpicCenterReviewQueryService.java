package com.opicer.api.opic.application;

import com.opicer.api.opic.domain.OpicCenterReview;
import com.opicer.api.opic.infrastructure.OpicCenterReviewRepository;
import com.opicer.api.opic.infrastructure.OpicTestCenterRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OpicCenterReviewQueryService {

	private final OpicTestCenterRepository centerRepository;
	private final OpicCenterReviewRepository reviewRepository;

	public OpicCenterReviewQueryService(OpicTestCenterRepository centerRepository,
		OpicCenterReviewRepository reviewRepository) {
		this.centerRepository = centerRepository;
		this.reviewRepository = reviewRepository;
	}

	/** Public view: visible reviews of an active center, newest first. Inactive centers are hidden like in the list. */
	public List<OpicCenterReview> visibleReviews(UUID centerId) {
		if (!centerRepository.existsByIdAndActiveTrue(centerId)) {
			throw centerNotFound(centerId);
		}
		return reviewRepository.findByCenterIdAndVisibleTrueOrderByCreatedAtDesc(centerId);
	}

	/** Admin view: every review including hidden ones, newest first; inactive centers stay manageable. */
	public List<OpicCenterReview> allReviews(UUID centerId) {
		if (!centerRepository.existsById(centerId)) {
			throw centerNotFound(centerId);
		}
		return reviewRepository.findByCenterIdOrderByCreatedAtDesc(centerId);
	}

	private static ApiException centerNotFound(UUID centerId) {
		return new ApiException(ErrorCode.OPIC_CENTER_NOT_FOUND, "OPIc test center not found: " + centerId);
	}
}
