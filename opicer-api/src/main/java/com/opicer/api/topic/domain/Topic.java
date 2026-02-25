package com.opicer.api.topic.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "topic")
public class Topic {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String englishTitle;

	@Column(nullable = false)
	private String category;

	@Column(nullable = false)
	private int categoryOrder;

	@Column(nullable = false)
	private int topicOrder;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "topic_badge", joinColumns = @JoinColumn(name = "topic_id"))
	@OrderColumn(name = "badge_order")
	private List<TopicBadge> badges = new ArrayList<>();

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected Topic() {
	}

	public Topic(String title, String englishTitle, String category, int categoryOrder, int topicOrder,
		List<TopicBadge> badges, boolean active) {
		this.title = title;
		this.englishTitle = englishTitle;
		this.category = category;
		this.categoryOrder = categoryOrder;
		this.topicOrder = topicOrder;
		this.badges = badges != null ? new ArrayList<>(badges) : new ArrayList<>();
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

	public void update(String title, String englishTitle, String category, int categoryOrder, int topicOrder,
		List<TopicBadge> badges, boolean active) {
		this.title = title;
		this.englishTitle = englishTitle;
		this.category = category;
		this.categoryOrder = categoryOrder;
		this.topicOrder = topicOrder;
		this.badges = badges != null ? new ArrayList<>(badges) : new ArrayList<>();
		this.active = active;
	}

	public UUID getId() { return id; }
	public String getTitle() { return title; }
	public String getEnglishTitle() { return englishTitle; }
	public String getCategory() { return category; }
	public int getCategoryOrder() { return categoryOrder; }
	public int getTopicOrder() { return topicOrder; }
	public List<TopicBadge> getBadges() { return badges; }
	public boolean isActive() { return active; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
