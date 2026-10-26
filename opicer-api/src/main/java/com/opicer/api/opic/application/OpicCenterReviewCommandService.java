package com.opicer.api.opic.application;

import com.opicer.api.opic.domain.OpicCenterReview;
import com.opicer.api.opic.infrastructure.OpicCenterReviewRepository;
import com.opicer.api.opic.infrastructure.OpicTestCenterRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpicCenterReviewCommandService {

	private final OpicTestCenterRepository centerRepository;
	private final OpicCenterReviewRepository reviewRepository;

	public OpicCenterReviewCommandService(OpicTestCenterRepository centerRepository,
		OpicCenterReviewRepository reviewRepository) {
		this.centerRepository = centerRepository;
		this.reviewRepository = reviewRepository;
	}

	@Transactional
	public OpicCenterReview create(UUID centerId, int rating, String content, String sourceLabel, boolean visible) {
		if (!centerRepository.existsById(centerId)) {
			throw new ApiException(ErrorCode.OPIC_CENTER_NOT_FOUND, "OPIc test center not found: " + centerId);
		}
		return reviewRepository.save(new OpicCenterReview(centerId, rating, content.trim(), blankToNull(sourceLabel), visible));
	}

	@Transactional
	public Optional<OpicCenterReview> update(UUID id, int rating, String content, String sourceLabel, boolean visible) {
		return reviewRepository.findById(id).map(review -> {
			review.update(rating, content.trim(), blankToNull(sourceLabel), visible);
			return reviewRepository.save(review);
		});
	}

	@Transactional
	public boolean delete(UUID id) {
		if (!reviewRepository.existsById(id)) {
			return false;
		}
		reviewRepository.deleteById(id);
		return true;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
