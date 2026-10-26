package com.opicer.api.opic.application;

import com.opicer.api.opic.application.OpicQueryService.CenterSummary;
import com.opicer.api.opic.domain.OpicCenterReview;
import com.opicer.api.opic.domain.OpicTestCenter;
import com.opicer.api.opic.infrastructure.OpicCenterReviewRepository;
import com.opicer.api.opic.infrastructure.OpicTestCenterRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OpicCenterReviewServiceTest {

	@Autowired
	private OpicCenterReviewCommandService commandService;

	@Autowired
	private OpicCenterReviewQueryService reviewQueryService;

	@Autowired
	private OpicQueryService opicQueryService;

	@Autowired
	private OpicTestCenterRepository centerRepository;

	@Autowired
	private OpicCenterReviewRepository reviewRepository;

	@BeforeEach
	void setUp() {
		reviewRepository.deleteAll();
		centerRepository.deleteAll();
	}

	@Test
	void publicViewHidesInvisibleReviewsAndSortsNewestFirst() {
		UUID centerId = center("강남센터").getId();
		OpicCenterReview older = commandService.create(centerId, 4, " 조용해요 ", " ", true);
		OpicCenterReview newer = commandService.create(centerId, 5, "친절해요", "응시자 제보", true);
		commandService.create(centerId, 1, "광고성 후기", null, false);
		backdate(older, 2);
		backdate(newer, 1);

		List<OpicCenterReview> visible = reviewQueryService.visibleReviews(centerId);
		assertThat(visible.stream().map(OpicCenterReview::getContent).toList()).containsExactly("친절해요", "조용해요");
		assertThat(visible.get(1).getSourceLabel()).isNull();

		assertThat(reviewQueryService.allReviews(centerId)).hasSize(3);
	}

	@Test
	void centerListCarriesVisibleReviewStats() {
		OpicTestCenter reviewed = center("강남센터");
		OpicTestCenter quiet = center("종로센터");
		commandService.create(reviewed.getId(), 5, "좋아요", null, true);
		commandService.create(reviewed.getId(), 4, "괜찮아요", null, true);
		commandService.create(reviewed.getId(), 4, "무난해요", null, true);
		commandService.create(reviewed.getId(), 1, "숨김", null, false);

		List<CenterSummary> centers = opicQueryService.centers("서울");

		CenterSummary reviewedSummary = summaryOf(centers, reviewed);
		assertThat(reviewedSummary.reviewCount()).isEqualTo(3);
		assertThat(reviewedSummary.averageRating()).isEqualTo(4.3);
		CenterSummary quietSummary = summaryOf(centers, quiet);
		assertThat(quietSummary.reviewCount()).isZero();
		assertThat(quietSummary.averageRating()).isNull();
	}

	@Test
	void updateAndDeleteReview() {
		UUID centerId = center("강남센터").getId();
		OpicCenterReview review = commandService.create(centerId, 3, "보통", null, true);

		assertThat(commandService.update(review.getId(), 2, "의자가 불편해요", "운영자 확인", false))
			.hasValueSatisfying(updated -> {
				assertThat(updated.getRating()).isEqualTo(2);
				assertThat(updated.isVisible()).isFalse();
			});
		assertThat(reviewQueryService.visibleReviews(centerId)).isEmpty();

		assertThat(commandService.delete(review.getId())).isTrue();
		assertThat(commandService.delete(review.getId())).isFalse();
		assertThat(commandService.update(review.getId(), 3, "x", null, true)).isEmpty();
	}

	@Test
	void inactiveCenterReviewsAreAdminOnly() {
		OpicTestCenter center = center("폐쇄센터");
		commandService.create(center.getId(), 4, "예전엔 좋았어요", null, true);
		center.deactivate();
		centerRepository.save(center);

		assertThatThrownBy(() -> reviewQueryService.visibleReviews(center.getId()))
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OPIC_CENTER_NOT_FOUND));
		assertThat(reviewQueryService.allReviews(center.getId())).hasSize(1);
	}

	@Test
	void unknownCenterIsRejected() {
		UUID missing = UUID.randomUUID();
		assertThatThrownBy(() -> commandService.create(missing, 3, "x", null, true))
			.isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OPIC_CENTER_NOT_FOUND));
		assertThatThrownBy(() -> reviewQueryService.visibleReviews(missing)).isInstanceOf(ApiException.class);
	}

	private OpicTestCenter center(String name) {
		OpicTestCenter center = new OpicTestCenter(name + "|서울");
		center.apply(name, "서울 어딘가", null, null, null, null, null, "C001", "서울", Instant.now());
		return centerRepository.save(center);
	}

	/** createdAt comes from @PrePersist; pin it so ordering does not depend on clock resolution. */
	private void backdate(OpicCenterReview review, int hoursAgo) {
		OpicCenterReview managed = reviewRepository.findById(review.getId()).orElseThrow();
		ReflectionTestUtils.setField(managed, "createdAt", Instant.now().minus(hoursAgo, ChronoUnit.HOURS));
		reviewRepository.saveAndFlush(managed);
	}

	private static CenterSummary summaryOf(List<CenterSummary> centers, OpicTestCenter center) {
		return centers.stream().filter(s -> s.center().getId().equals(center.getId())).findFirst().orElseThrow();
	}
}
