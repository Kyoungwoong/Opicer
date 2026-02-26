package com.opicer.api.goodanswer.domain;

import com.opicer.api.shared.domain.OpicLevel;
import com.opicer.api.shared.infrastructure.PgVectorConverter;
import com.opicer.api.topic.domain.Topic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "good_answer_sample")
public class GoodAnswerSample {

	@Id
	@GeneratedValue
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "topic_id", nullable = false)
	private Topic topic;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OpicLevel level;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String sampleText;

	@Column
	private String sampleAudioUrl;

	@Column(columnDefinition = "TEXT")
	private String summary;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "good_answer_sample_tags", joinColumns = @JoinColumn(name = "sample_id"))
	@Column(name = "tag", nullable = false)
	private List<String> tags = new ArrayList<>();

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "good_answer_sample_expressions", joinColumns = @JoinColumn(name = "sample_id"))
	@Column(name = "expression", nullable = false)
	private List<String> keyExpressions = new ArrayList<>();

	@Convert(converter = PgVectorConverter.class)
	@Column(columnDefinition = "vector(1536)")
	private float[] embedding;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected GoodAnswerSample() {
	}

	public GoodAnswerSample(Topic topic, OpicLevel level, String sampleText, String sampleAudioUrl,
		String summary, List<String> tags, List<String> keyExpressions, float[] embedding) {
		this.topic = topic;
		this.level = level;
		this.sampleText = sampleText;
		this.sampleAudioUrl = sampleAudioUrl;
		this.summary = summary;
		this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
		this.keyExpressions = keyExpressions != null ? new ArrayList<>(keyExpressions) : new ArrayList<>();
		this.embedding = embedding;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public Topic getTopic() { return topic; }
	public OpicLevel getLevel() { return level; }
	public String getSampleText() { return sampleText; }
	public String getSampleAudioUrl() { return sampleAudioUrl; }
	public String getSummary() { return summary; }
	public List<String> getTags() { return tags; }
	public List<String> getKeyExpressions() { return keyExpressions; }
	public float[] getEmbedding() { return embedding; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
