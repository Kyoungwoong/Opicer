package com.opicer.api.mocktest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/**
 * One practice topic a Background Survey item points to. The mock test picks its survey combos from the topics the
 * learner's choices link to. Links hold the topic id, so a renamed topic keeps its links.
 */
@Entity
@Table(
	name = "survey_topic_link",
	uniqueConstraints = @UniqueConstraint(name = "uk_survey_topic_link", columnNames = {"item_id", "topic_id"}),
	indexes = @Index(name = "idx_survey_topic_link_topic", columnList = "topic_id")
)
public class SurveyTopicLink {

	@Id
	@GeneratedValue
	private UUID id;

	/** A {@link SurveyCatalog} item id, e.g. {@code leisure.movies}. */
	@Column(name = "item_id", nullable = false, length = 64)
	private String itemId;

	@Column(name = "topic_id", nullable = false)
	private UUID topicId;

	protected SurveyTopicLink() {
	}

	public SurveyTopicLink(String itemId, UUID topicId) {
		this.itemId = itemId;
		this.topicId = topicId;
	}

	public UUID getId() { return id; }
	public String getItemId() { return itemId; }
	public UUID getTopicId() { return topicId; }
}
