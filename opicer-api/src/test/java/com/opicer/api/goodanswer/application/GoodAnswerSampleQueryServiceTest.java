package com.opicer.api.goodanswer.application;

import com.opicer.api.ai.application.AiEmbeddingService;
import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.goodanswer.infrastructure.GoodAnswerSampleRepository;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class GoodAnswerSampleQueryServiceTest {

	private static final String ANSWER =
		"I live in a small apartment near the river and I really like my neighborhood because it is quiet";

	@Autowired
	private GoodAnswerSampleQueryService queryService;

	@Autowired
	private GoodAnswerSampleRepository sampleRepository;

	@Autowired
	private TopicRepository topicRepository;

	@MockBean
	private AiEmbeddingService embeddingService;

	private Topic topic;

	@BeforeEach
	void setUp() {
		sampleRepository.deleteAll();
		topic = topicRepository.save(new Topic("집", "Home", "rag-test", 1, 1, List.of(), true));
	}

	@AfterEach
	void tearDown() {
		sampleRepository.deleteAll();
		topicRepository.delete(topic);
	}

	@Test
	void returnsDiverseRelevantSamplesAboveTheFloorAsDetachedRecords() {
		save("a1", List.of("cozy"), vector(1, 0, 0.3f, 0));
		save("a2 near-duplicate of a1", List.of(), vector(1, 0, 0.3f, 0.02f));
		save("b different angle", List.of("commute"), vector(1, 0.35f, 0, 0));
		save("unrelated", List.of(), vector(0, 1, 0, 0));
		when(embeddingService.embed(ANSWER)).thenReturn(vector(1, 0, 0, 0));

		List<ReferenceSample> samples = queryService.findReferenceSamples(topic.getId(), ANSWER);

		// MMR keeps a1, swaps the near-duplicate for b; "unrelated" (similarity 0) is below the 0.25 floor.
		assertThat(samples).extracting(ReferenceSample::sampleText)
			.containsExactly("a1", "b different angle", "a2 near-duplicate of a1");
		assertThat(samples.get(0).similarity()).isGreaterThan(0.9);
		// Lazy collection was read inside the transaction, so it is usable here.
		assertThat(samples.get(0).keyExpressions()).containsExactly("cozy");
		assertThat(samples.get(0).level()).isEqualTo(OpicLevel.IH);
	}

	@Test
	void skipsRetrievalForAnswersTooShortToCompare() {
		assertThat(queryService.findReferenceSamples(topic.getId(), "I like my home")).isEmpty();
		verify(embeddingService, never()).embed(anyString());
	}

	private void save(String text, List<String> expressions, float[] embedding) {
		sampleRepository.save(new GoodAnswerSample(topic, OpicLevel.IH, text, null, null, List.of(), expressions, embedding));
	}

	private static float[] vector(float... head) {
		float[] v = new float[1536];
		System.arraycopy(head, 0, v, 0, head.length);
		return v;
	}
}
