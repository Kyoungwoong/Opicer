package com.opicer.api.evaluationguide.domain;

import com.opicer.api.evaluation.domain.EvaluationGuideKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * One version of an admin-written markdown guide that the evaluator reads. At most one version per kind is
 * active; {@code activeKind} mirrors the kind only while active, so a plain unique column enforces that in the
 * database (Postgres treats NULLs as distinct). An active version is frozen: evaluations record which version
 * they used, so its text must not change underneath them.
 */
@Entity
@Table(name = "evaluation_guide", uniqueConstraints = {
	@UniqueConstraint(name = "uk_evaluation_guide_kind_version", columnNames = {"kind", "version"}),
	@UniqueConstraint(name = "uk_evaluation_guide_active_kind", columnNames = {"active_kind"})
})
public class EvaluationGuide {

	public static final int MAX_TITLE_LENGTH = 100;
	/** Keeps the prompt bounded even with every kind active. */
	public static final int MAX_CONTENT_LENGTH = 4000;

	@Id
	@GeneratedValue
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private EvaluationGuideKind kind;

	@Column(nullable = false)
	private int version;

	@Column(nullable = false, length = MAX_TITLE_LENGTH)
	private String title;

	@Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
	private String contentMd;

	@Column(nullable = false)
	private boolean active = false;

	@Enumerated(EnumType.STRING)
	@Column(name = "active_kind", length = 32)
	private EvaluationGuideKind activeKind;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected EvaluationGuide() {
	}

	public EvaluationGuide(EvaluationGuideKind kind, int version, String title, String contentMd) {
		this.kind = kind;
		this.version = version;
		this.title = title;
		this.contentMd = contentMd;
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

	/** Callers check {@link #isActive()} first; an active version is frozen. */
	public void edit(String title, String contentMd) {
		if (active) {
			throw new IllegalStateException("an active guide cannot be edited");
		}
		this.title = title;
		this.contentMd = contentMd;
	}

	public void activate() {
		this.active = true;
		this.activeKind = kind;
	}

	public void deactivate() {
		this.active = false;
		this.activeKind = null;
	}

	public UUID getId() { return id; }
	public EvaluationGuideKind getKind() { return kind; }
	public int getVersion() { return version; }
	public String getTitle() { return title; }
	public String getContentMd() { return contentMd; }
	public boolean isActive() { return active; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
