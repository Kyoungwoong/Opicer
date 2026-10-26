package com.opicer.api.goodanswer.infrastructure;

import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import java.util.List;
import java.util.UUID;

public interface GoodAnswerSampleRepositoryCustom {
	/** Exact cosine-distance nearest neighbours within one topic; samples without an embedding are skipped. */
	List<GoodAnswerSample> findNearest(UUID topicId, float[] embedding, int limit);
}
