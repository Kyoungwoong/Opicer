package com.opicer.api.opic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One OPIc exam day as published on opic.or.kr (facts only: dates and round name). */
@Entity
@Table(
	name = "opic_exam_schedule",
	uniqueConstraints = @UniqueConstraint(name = "uk_opic_exam_schedule_external_key", columnNames = {"external_key"}),
	indexes = @Index(name = "idx_opic_exam_schedule_exam_date", columnList = "exam_date")
)
public class OpicExamSchedule {

	@Id
	@GeneratedValue
	private UUID id;

	/** opic.or.kr round id + subject code, e.g. "6553-OPI001". */
	@Column(name = "external_key", nullable = false, length = 64)
	private String externalKey;

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

	@Column(name = "synced_at", nullable = false)
	private Instant syncedAt;

	protected OpicExamSchedule() {
	}

	public OpicExamSchedule(String externalKey) {
		this.externalKey = externalKey;
	}

	public void apply(LocalDate examDate, LocalDate registrationStart, LocalDate registrationEnd,
		LocalDate scoreReleaseDate, String roundName, boolean earlyRelease, Instant syncedAt) {
		this.examDate = examDate;
		this.registrationStart = registrationStart;
		this.registrationEnd = registrationEnd;
		this.scoreReleaseDate = scoreReleaseDate;
		this.roundName = roundName;
		this.earlyRelease = earlyRelease;
		this.syncedAt = syncedAt;
	}

	public UUID getId() { return id; }
	public String getExternalKey() { return externalKey; }
	public LocalDate getExamDate() { return examDate; }
	public LocalDate getRegistrationStart() { return registrationStart; }
	public LocalDate getRegistrationEnd() { return registrationEnd; }
	public LocalDate getScoreReleaseDate() { return scoreReleaseDate; }
	public String getRoundName() { return roundName; }
	public boolean isEarlyRelease() { return earlyRelease; }
	public Instant getSyncedAt() { return syncedAt; }
}
