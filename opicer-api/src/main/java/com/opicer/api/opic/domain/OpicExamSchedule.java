package com.opicer.api.opic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One OPIc exam round shown on the schedule calendar: dates and round name only. Opicer does not collect these from
 * opic.or.kr (its robots.txt refuses automated collection); rows are entered directly for now, and a permitted source
 * can later fill the same table, recording itself in {@code source}.
 */
@Entity
@Table(
	name = "opic_schedule",
	uniqueConstraints = @UniqueConstraint(name = "uk_opic_schedule_round", columnNames = {"exam_date", "round_name"}),
	indexes = @Index(name = "idx_opic_schedule_exam_date", columnList = "exam_date")
)
public class OpicExamSchedule {

	public static final String SOURCE_MANUAL = "manual";

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "exam_date", nullable = false)
	private LocalDate examDate;

	@Column(name = "registration_start")
	private LocalDate registrationStart;

	@Column(name = "registration_end")
	private LocalDate registrationEnd;

	@Column(name = "score_release_date")
	private LocalDate scoreReleaseDate;

	@Column(name = "round_name", nullable = false)
	private String roundName;

	@Column(name = "early_release", nullable = false)
	private boolean earlyRelease;

	/** Where the row came from: {@code manual} or the name of a permitted import. */
	@Column(nullable = false, length = 64)
	private String source;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected OpicExamSchedule() {
	}

	public OpicExamSchedule(LocalDate examDate, LocalDate registrationStart, LocalDate registrationEnd,
		LocalDate scoreReleaseDate, String roundName, boolean earlyRelease, String source) {
		this.examDate = examDate;
		this.registrationStart = registrationStart;
		this.registrationEnd = registrationEnd;
		this.scoreReleaseDate = scoreReleaseDate;
		this.roundName = roundName;
		this.earlyRelease = earlyRelease;
		this.source = source;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public LocalDate getExamDate() { return examDate; }
	public LocalDate getRegistrationStart() { return registrationStart; }
	public LocalDate getRegistrationEnd() { return registrationEnd; }
	public LocalDate getScoreReleaseDate() { return scoreReleaseDate; }
	public String getRoundName() { return roundName; }
	public boolean isEarlyRelease() { return earlyRelease; }
	public String getSource() { return source; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
