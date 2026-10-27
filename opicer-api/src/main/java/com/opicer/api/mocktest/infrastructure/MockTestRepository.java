package com.opicer.api.mocktest.infrastructure;

import com.opicer.api.mocktest.domain.MockTest;
import com.opicer.api.mocktest.domain.MockTestStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MockTestRepository extends JpaRepository<MockTest, UUID> {

	Optional<MockTest> findByIdAndUserId(UUID id, UUID userId);

	/** Serialises adjust/submit of one test, so the credit is charged at most once. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from MockTest t where t.id = :id and t.userId = :userId")
	Optional<MockTest> findLockedByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

	List<MockTest> findByUserIdAndStatusOrderBySubmittedAtDesc(UUID userId, MockTestStatus status, Pageable page);

	/** Earlier submitted tests, newest first, for the progress trend. */
	List<MockTest> findByUserIdAndStatusAndSubmittedAtBeforeOrderBySubmittedAtDesc(UUID userId, MockTestStatus status,
		Instant before, Pageable page);
}
