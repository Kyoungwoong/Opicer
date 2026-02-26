package com.opicer.api.goodanswer.infrastructure;

import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodAnswerSampleRepository extends JpaRepository<GoodAnswerSample, UUID>,
	GoodAnswerSampleRepositoryCustom {

	List<GoodAnswerSample> findByTopicId(UUID topicId);
}
