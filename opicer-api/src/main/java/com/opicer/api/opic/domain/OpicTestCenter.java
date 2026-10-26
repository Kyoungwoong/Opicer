package com.opicer.api.opic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Locale;
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

	/** Last change by an admin (the column name predates manual entry). */
	@Column(name = "synced_at", nullable = false)
	private Instant syncedAt;

	protected OpicTestCenter() {
	}

	/** A new, active center entered by an admin; coordinates are set separately with {@link #locate}. */
	public OpicTestCenter(String name, String address, String phone, String subwayLines, String directions,
		String regionName, Instant now) {
		update(name, address, phone, subwayLines, directions, regionName, now);
		this.active = true;
	}

	/** Same center if the name and address match after trimming, collapsing spaces and ignoring case. */
	public static String naturalKey(String name, String address) {
		return normalize(name) + "|" + normalize(address);
	}

	public void update(String name, String address, String phone, String subwayLines, String directions,
		String regionName, Instant now) {
		this.externalKey = naturalKey(name, address);
		this.name = name.trim();
		this.address = address.trim();
		this.phone = blankToNull(phone);
		this.subwayLines = blankToNull(subwayLines);
		this.directions = blankToNull(directions);
		this.regionName = blankToNull(regionName);
		this.syncedAt = now;
	}

	/** Whether {@code other} is this center's address, ignoring spacing and case. */
	public boolean hasAddress(String other) {
		return normalize(address).equals(normalize(other));
	}

	/** Both null clears the position (the map then skips the center). */
	public void locate(Double latitude, Double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void activate() {
		this.active = true;
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

	private static String normalize(String value) {
		return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
