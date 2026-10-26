package com.opicer.api.goodanswer.infrastructure;

import com.opicer.api.goodanswer.domain.GoodAnswerSample;
import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.topic.domain.Topic;
import com.opicer.api.topic.infrastructure.TopicRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/** Runs the cosine-distance SQL against real pgvector with synthetic vectors. */
@SpringBootTest
class GoodAnswerSampleRepositoryTest {

	@Autowired
	private GoodAnswerSampleRepository sampleRepository;

	@Autowired
	private TopicRepository topicRepository;

	private Topic topic;
	private Topic otherTopic;

	@BeforeEach
	void setUp() {
		sampleRepository.deleteAll();
		topic = topicRepository.save(new Topic("집", "Home", "rag-test", 1, 1, List.of(), true));
		otherTopic = topicRepository.save(new Topic("여행", "Travel", "rag-test", 1, 2, List.of(), true));
	}

	@AfterEach
	void tearDown() {
		sampleRepository.deleteAll();
		topicRepository.deleteAll(List.of(topic, otherTopic));
	}

	@Test
	void ordersByCosineDistanceWithinTheTopicAndSkipsMissingEmbeddings() {
		save(topic, "far", vector(0, 1, 0));
		save(topic, "same", vector(1, 0, 0));
		save(topic, "near", vector(1, 0.5f, 0));
		// Magnitude must not matter for cosine: a long vector in the query direction ranks with "same".
		save(topic, "same-but-long", vector(5, 0, 0));
		save(topic, "no-embedding", null);
		save(otherTopic, "other-topic", vector(1, 0, 0));

		List<GoodAnswerSample> nearest = sampleRepository.findNearest(topic.getId(), vector(1, 0, 0), 10);

		assertThat(nearest).extracting(GoodAnswerSample::getSampleText)
			.containsExactlyInAnyOrder("same", "same-but-long", "near", "far")
			.doesNotContain("no-embedding", "other-topic");
		assertThat(nearest.subList(0, 2)).extracting(GoodAnswerSample::getSampleText)
			.containsExactlyInAnyOrder("same", "same-but-long");
		assertThat(nearest.get(2).getSampleText()).isEqualTo("near");
		assertThat(nearest.get(3).getSampleText()).isEqualTo("far");
	}

	@Test
	void respectsTheCandidateLimit() {
		save(topic, "a", vector(1, 0, 0));
		save(topic, "b", vector(1, 1, 0));
		save(topic, "c", vector(0, 1, 0));

		assertThat(sampleRepository.findNearest(topic.getId(), vector(1, 0, 0), 2))
			.extracting(GoodAnswerSample::getSampleText)
			.containsExactly("a", "b");
	}

	private void save(Topic owner, String text, float[] embedding) {
		sampleRepository.save(new GoodAnswerSample(owner, OpicLevel.IM, text, null, null, List.of(), List.of(), embedding));
	}

	/** A 1536-dim vector (the column's size) whose leading components are given. */
	static float[] vector(float... head) {
		float[] v = new float[1536];
		System.arraycopy(head, 0, v, 0, head.length);
		return v;
	}
}
