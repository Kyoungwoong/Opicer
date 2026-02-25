package com.opicer.api.prompt.domain;

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
import java.util.UUID;

@Entity
@Table(name = "prompt_version")
public class PromptVersion {

	@Id
	@GeneratedValue
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PromptUseCase useCase;

	@Column(nullable = false)
	private int version;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String template;

	@Column(nullable = false)
	private boolean active = false;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected PromptVersion() {
	}

	public PromptVersion(PromptUseCase useCase, int version, String name, String template) {
		this.useCase = useCase;
		this.version = version;
		this.name = name;
		this.template = template;
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

	public void update(PromptUseCase useCase, int version, String name, String template) {
		this.useCase = useCase;
		this.version = version;
		this.name = name;
		this.template = template;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}

	public UUID getId() { return id; }
	public PromptUseCase getUseCase() { return useCase; }
	public int getVersion() { return version; }
	public String getName() { return name; }
	public String getTemplate() { return template; }
	public boolean isActive() { return active; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
