package com.opicer.api.opic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/** An OPIc test center. Centers are deactivated rather than deleted, so their reviews stay linked. */
@Entity
@Table(
	name = "opic_test_center",
	uniqueConstraints = @UniqueConstraint(name = "uk_opic_test_center_external_key", columnNames = {"external_key"})
)
public class OpicTestCenter {

	@Id
	@GeneratedValue
	private UUID id;

	/** Name + road address: the natural key that keeps the same center from being added twice. */
	@Column(name = "external_key", nullable = false, length = 600)
	private String externalKey;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, length = 500)
	private String address;

	private Double latitude;

	private Double longitude;

	private String phone;

	@Column(name = "subway_lines")
	private String subwayLines;

	@Column(columnDefinition = "TEXT")
	private String directions;

	@Column(name = "region_code", length = 16)
	private String regionCode;

	@Column(name = "region_name", length = 32)
	private String regionName;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "synced_at", nullable = false)
	private Instant syncedAt;

	protected OpicTestCenter() {
	}

	public OpicTestCenter(String externalKey) {
		this.externalKey = externalKey;
	}

	public void apply(String name, String address, Double latitude, Double longitude, String phone,
		String subwayLines, String directions, String regionCode, String regionName, Instant syncedAt) {
		this.name = name;
		this.address = address;
		this.latitude = latitude;
		this.longitude = longitude;
		this.phone = phone;
		this.subwayLines = subwayLines;
		this.directions = directions;
		this.regionCode = regionCode;
		this.regionName = regionName;
		this.active = true;
		this.syncedAt = syncedAt;
	}

	public void deactivate() {
		this.active = false;
	}

	public UUID getId() { return id; }
	public String getExternalKey() { return externalKey; }
	public String getName() { return name; }
	public String getAddress() { return address; }
	public Double getLatitude() { return latitude; }
	public Double getLongitude() { return longitude; }
	public String getPhone() { return phone; }
	public String getSubwayLines() { return subwayLines; }
	public String getDirections() { return directions; }
	public String getRegionCode() { return regionCode; }
	public String getRegionName() { return regionName; }
	public boolean isActive() { return active; }
	public Instant getSyncedAt() { return syncedAt; }
}
