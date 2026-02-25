package com.opicer.api.question.domain;

import com.opicer.api.shared.domain.OpicLevel;
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
@Table(name = "question")
public class Question {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false)
	private String topic;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private QuestionType type;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String promptText;

	@Column
	private String promptAudioUrl;

	@Column(columnDefinition = "TEXT")
	private String structuralHint;

	@ElementCollection(fetch = FetchType.LAZY)
	@Enumerated(EnumType.STRING)
	@CollectionTable(name = "question_target_levels", joinColumns = @JoinColumn(name = "question_id"))
	@Column(name = "level", nullable = false)
	private List<OpicLevel> targetLevels = new ArrayList<>();

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "question_key_expressions", joinColumns = @JoinColumn(name = "question_id"))
	@Column(name = "expression", nullable = false)
	private List<String> keyExpressions = new ArrayList<>();

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected Question() {
	}

	public Question(String topic, QuestionType type, String promptText, String promptAudioUrl,
		String structuralHint, List<OpicLevel> targetLevels, List<String> keyExpressions) {
		this.topic = topic;
		this.type = type;
		this.promptText = promptText;
		this.promptAudioUrl = promptAudioUrl;
		this.structuralHint = structuralHint;
		this.targetLevels = targetLevels != null ? new ArrayList<>(targetLevels) : new ArrayList<>();
		this.keyExpressions = keyExpressions != null ? new ArrayList<>(keyExpressions) : new ArrayList<>();
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

	public void update(String topic, QuestionType type, String promptText, String promptAudioUrl,
		String structuralHint, List<OpicLevel> targetLevels, List<String> keyExpressions, boolean active) {
		this.topic = topic;
		this.type = type;
		this.promptText = promptText;
		this.promptAudioUrl = promptAudioUrl;
		this.structuralHint = structuralHint;
		this.targetLevels = targetLevels != null ? new ArrayList<>(targetLevels) : new ArrayList<>();
		this.keyExpressions = keyExpressions != null ? new ArrayList<>(keyExpressions) : new ArrayList<>();
		this.active = active;
	}

	public UUID getId() { return id; }
	public String getTopic() { return topic; }
	public QuestionType getType() { return type; }
	public String getPromptText() { return promptText; }
	public String getPromptAudioUrl() { return promptAudioUrl; }
	public String getStructuralHint() { return structuralHint; }
	public List<OpicLevel> getTargetLevels() { return targetLevels; }
	public List<String> getKeyExpressions() { return keyExpressions; }
	public boolean isActive() { return active; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
