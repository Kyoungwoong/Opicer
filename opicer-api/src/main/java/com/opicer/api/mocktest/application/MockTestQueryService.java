package com.opicer.api.mocktest.application;

import com.opicer.api.mocktest.domain.MockTest;
import com.opicer.api.mocktest.domain.MockTestItem;
import com.opicer.api.mocktest.domain.MockTestStatus;
import com.opicer.api.mocktest.infrastructure.MockTestItemRepository;
import com.opicer.api.mocktest.infrastructure.MockTestRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MockTestQueryService {

	static final int MAX_LIMIT = 50;

	private final MockTestRepository tests;
	private final MockTestItemRepository items;

	public MockTestQueryService(MockTestRepository tests, MockTestItemRepository items) {
		this.tests = tests;
		this.items = items;
	}

	/** Submitted tests only, newest first; an abandoned test is never part of the history. */
	public List<Summary> list(UUID userId, int limit) {
		List<MockTest> submitted = tests.findByUserIdAndStatusOrderBySubmittedAtDesc(userId, MockTestStatus.SUBMITTED,
			PageRequest.of(0, Math.max(1, Math.min(limit, MAX_LIMIT))));
		Map<UUID, Long> counts = items.findByMockTestIdIn(submitted.stream().map(MockTest::getId).toList()).stream()
			.collect(Collectors.groupingBy(MockTestItem::getMockTestId, Collectors.counting()));
		return submitted.stream().map(t -> new Summary(t, counts.getOrDefault(t.getId(), 0L).intValue())).toList();
	}

	public Detail get(UUID userId, UUID mockTestId) {
		MockTest test = tests.findByIdAndUserId(mockTestId, userId)
			.orElseThrow(() -> new ApiException(ErrorCode.MOCK_TEST_NOT_FOUND));
		return new Detail(test, items.findByMockTestIdOrderByItemOrderAsc(test.getId()));
	}

	public record Summary(MockTest test, int itemCount) {}

	public record Detail(MockTest test, List<MockTestItem> items) {}
}
