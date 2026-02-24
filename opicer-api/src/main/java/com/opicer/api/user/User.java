package com.opicer.api.user;

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

@Entity
@Table(name = "app_user", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"provider", "providerId"})
})
public class User {

	@Id
	@GeneratedValue
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuthProvider provider;

	@Column(nullable = false)
	private String providerId;

	@Column
	private String email;

	@Column
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected User() {
	}

	public User(AuthProvider provider, String providerId, String email, String name, UserRole role) {
		this.provider = provider;
		this.providerId = providerId;
		this.email = email;
		this.name = name;
		this.role = role;
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

	public UUID getId() {
		return id;
	}

	public AuthProvider getProvider() {
		return provider;
	}

	public String getProviderId() {
		return providerId;
	}

	public String getEmail() {
		return email;
	}

	public String getName() {
		return name;
	}

	public UserRole getRole() {
		return role;
	}

	public void updateProfile(String email, String name, UserRole role) {
		this.email = email;
		this.name = name;
		this.role = role;
	}
}
