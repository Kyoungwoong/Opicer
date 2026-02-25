package com.opicer.api.dailysentence.domain;

import com.opicer.api.shared.domain.OpicLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_sentence")
public class DailySentence {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false, unique = true)
	private LocalDate date;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String text;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OpicLevel level;

	@Column
	private String audioUrl;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected DailySentence() {
	}

	public DailySentence(LocalDate date, String text, OpicLevel level, String audioUrl) {
		this.date = date;
		this.text = text;
		this.level = level;
		this.audioUrl = audioUrl;
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

	public void update(LocalDate date, String text, OpicLevel level, String audioUrl, boolean active) {
		this.date = date;
		this.text = text;
		this.level = level;
		this.audioUrl = audioUrl;
		this.active = active;
	}

	public UUID getId() { return id; }
	public LocalDate getDate() { return date; }
	public String getText() { return text; }
	public OpicLevel getLevel() { return level; }
	public String getAudioUrl() { return audioUrl; }
	public boolean isActive() { return active; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
