package com.opicer.api.goodanswer.infrastructure;

import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import java.util.List;
import java.util.UUID;

public interface GoodAnswerSampleRepositoryCustom {
	List<GoodAnswerSample> findSimilar(UUID topicId, float[] embedding, int limit);
}
