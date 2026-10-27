package com.opicer.api.mocktest.application;

import com.opicer.api.mocktest.domain.MockTest;
import com.opicer.api.mocktest.domain.MockTestItem;
import com.opicer.api.mocktest.domain.MockTestStatus;
import com.opicer.api.mocktest.infrastructure.MockTestItemRepository;
import com.opicer.api.mocktest.infrastructure.MockTestRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opicer.api.shared.domain.OpicLevel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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
	static final int TREND_SIZE = 5;
	private static final ObjectMapper JSON = new ObjectMapper();

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
		return new Detail(test, items.findByMockTestIdOrderByItemOrderAsc(test.getId()), trend(userId, test));
	}

	/**
	 * Up to {@value #TREND_SIZE} earlier evaluated tests and this one, oldest first, so the screen can draw how the
	 * estimate moved. Empty until this test has a level.
	 */
	private List<TrendPoint> trend(UUID userId, MockTest test) {
		if (test.getSubmittedAt() == null || test.getEstimatedLevel() == null) {
			return List.of();
		}
		List<MockTest> earlier = tests.findByUserIdAndStatusAndSubmittedAtBeforeOrderBySubmittedAtDesc(userId,
			MockTestStatus.SUBMITTED, test.getSubmittedAt(), PageRequest.of(0, TREND_SIZE * 2));
		List<TrendPoint> points = new ArrayList<>(earlier.stream()
			.filter(t -> t.getEstimatedLevel() != null)
			.limit(TREND_SIZE)
			.map(MockTestQueryService::point)
			.toList());
		Collections.reverse(points);
		points.add(point(test));
		return points;
	}

	private static TrendPoint point(MockTest test) {
		Map<String, String> dimensions = new LinkedHashMap<>();
		if (test.getReport() != null) {
			try {
				JSON.readTree(test.getReport()).path("dimensionLevels").fields()
					.forEachRemaining(e -> dimensions.put(e.getKey(), e.getValue().asText()));
			} catch (JsonProcessingException e) {
				throw new IllegalStateException("stored report is not valid JSON", e);
			}
		}
		return new TrendPoint(test.getId(), test.getSubmittedAt(), test.getEstimatedLevel(), dimensions);
	}

	public record Summary(MockTest test, int itemCount) {}

	public record Detail(MockTest test, List<MockTestItem> items, List<TrendPoint> trend) {}

	/** @param dimensionLevels competency code → level, from the report */
	public record TrendPoint(UUID id, Instant submittedAt, OpicLevel estimatedLevel, Map<String, String> dimensionLevels) {}
}
