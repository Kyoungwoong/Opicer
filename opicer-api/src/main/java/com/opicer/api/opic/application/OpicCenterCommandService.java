package com.opicer.api.opic.application;

import com.opicer.api.opic.domain.OpicTestCenter;
import com.opicer.api.opic.infrastructure.OpicTestCenterRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-entered test centers. Coordinates come from the admin when given, otherwise from geocoding the address;
 * a geocoding problem never blocks saving, it only comes back as a warning.
 */
@Service
public class OpicCenterCommandService {

	private final OpicTestCenterRepository repository;
	private final Geocoder geocoder;
	private final Clock clock;

	public OpicCenterCommandService(OpicTestCenterRepository repository, Geocoder geocoder, Clock clock) {
		this.repository = repository;
		this.geocoder = geocoder;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<OpicTestCenter> findAll() {
		return repository.findAllByOrderByRegionNameAscNameAsc();
	}

	@Transactional
	public Saved create(CenterInput input) {
		input.validate();
		String key = OpicTestCenter.naturalKey(input.name(), input.address());
		if (repository.existsByExternalKey(key)) {
			throw new ApiException(ErrorCode.OPIC_CENTER_DUPLICATE);
		}
		OpicTestCenter center = new OpicTestCenter(input.name(), input.address(), input.phone(), input.subwayLines(),
			input.directions(), input.regionName(), Instant.now(clock));
		String warning = position(center, input, true);
		return new Saved(repository.save(center), warning);
	}

	/** Geocodes again only when the address changed and no coordinates were given. */
	@Transactional
	public Saved update(UUID id, CenterInput input) {
		input.validate();
		OpicTestCenter center = find(id);
		String key = OpicTestCenter.naturalKey(input.name(), input.address());
		if (repository.existsByExternalKeyAndIdNot(key, id)) {
			throw new ApiException(ErrorCode.OPIC_CENTER_DUPLICATE);
		}
		boolean addressChanged = !center.hasAddress(input.address());
		center.update(input.name(), input.address(), input.phone(), input.subwayLines(), input.directions(),
			input.regionName(), Instant.now(clock));
		String warning = position(center, input, addressChanged);
		return new Saved(center, warning);
	}

	@Transactional
	public OpicTestCenter setActive(UUID id, boolean active) {
		OpicTestCenter center = find(id);
		if (active) {
			center.activate();
		} else {
			center.deactivate();
		}
		return center;
	}

	/** @return a warning for the admin, or null */
	private String position(OpicTestCenter center, CenterInput input, boolean lookUp) {
		if (input.hasCoordinates()) {
			center.locate(input.latitude(), input.longitude());
			return null;
		}
		if (!lookUp && center.getLatitude() != null) {
			return null;
		}
		Geocoder.Result result = geocoder.geocode(input.address().trim());
		center.locate(result.latitude(), result.longitude());
		return result.isFound() ? null : result.failure();
	}

	private OpicTestCenter find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.OPIC_CENTER_NOT_FOUND));
	}

	/** {@code latitude} and {@code longitude} are both given or both null. */
	public record CenterInput(String name, String address, String phone, String subwayLines, String directions,
		String regionName, Double latitude, Double longitude) {

		boolean hasCoordinates() {
			return latitude != null && longitude != null;
		}

		void validate() {
			if ((latitude == null) != (longitude == null)) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "latitude and longitude go together");
			}
			if (hasCoordinates() && (Math.abs(latitude) > 90 || Math.abs(longitude) > 180)) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "coordinates are out of range");
			}
		}
	}

	/** @param warning set when the center was saved without coordinates */
	public record Saved(OpicTestCenter center, String warning) {}
}
