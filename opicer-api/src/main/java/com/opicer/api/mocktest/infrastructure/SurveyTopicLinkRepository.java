package com.opicer.api.mocktest.infrastructure;

import com.opicer.api.mocktest.domain.SurveyTopicLink;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurveyTopicLinkRepository extends JpaRepository<SurveyTopicLink, UUID> {

	List<SurveyTopicLink> findByItemIdIn(Collection<String> itemIds);

	boolean existsByTopicId(UUID topicId);

	@Query("select distinct l.topicId from SurveyTopicLink l")
	Set<UUID> findLinkedTopicIds();

	@Modifying
	@Query("delete from SurveyTopicLink l where l.itemId = :itemId")
	int deleteByItemId(@Param("itemId") String itemId);

	@Modifying
	@Query("delete from SurveyTopicLink l where l.topicId = :topicId")
	int deleteByTopicId(@Param("topicId") UUID topicId);
}
