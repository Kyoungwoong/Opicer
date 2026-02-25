package com.opicer.api.universalsentence.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "universal_sentence")
public class UniversalSentence {

	@Id
	@GeneratedValue
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UniversalSentenceType type;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String sentence;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "universal_sentence_tags", joinColumns = @JoinColumn(name = "sentence_id"))
	@Column(name = "tag", nullable = false)
	private List<String> tags = new ArrayList<>();

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected UniversalSentence() {
	}

	public UniversalSentence(UniversalSentenceType type, String title, String sentence, List<String> tags) {
		this(type, title, sentence, tags, true);
	}

	public UniversalSentence(UniversalSentenceType type, String title, String sentence, List<String> tags, boolean active) {
		this.type = type;
		this.title = title;
		this.sentence = sentence;
		this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
		this.active = active;
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

	public void update(UniversalSentenceType type, String title, String sentence, List<String> tags, boolean active) {
		this.type = type;
		this.title = title;
		this.sentence = sentence;
		this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
		this.active = active;
	}

	public UUID getId() { return id; }
	public UniversalSentenceType getType() { return type; }
	public String getTitle() { return title; }
	public String getSentence() { return sentence; }
	public List<String> getTags() { return tags; }
	public boolean isActive() { return active; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
