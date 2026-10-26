package com.opicer.api.opic.application;

import com.opicer.api.opic.application.OpicCenterCommandService.CenterInput;
import com.opicer.api.opic.application.OpicCenterCommandService.Saved;
import com.opicer.api.opic.domain.OpicTestCenter;
import com.opicer.api.opic.infrastructure.OpicCenterReviewRepository;
import com.opicer.api.opic.infrastructure.OpicTestCenterRepository;
import com.opicer.api.shared.error.ApiException;
import com.opicer.api.shared.error.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Admin-entered centers on real Postgres; the geocoder is a mock. */
@SpringBootTest
class OpicCenterCommandServiceTest {

	private static final String ADDRESS = "서울 강남구 테헤란로 123";

	@Autowired
	private OpicCenterCommandService service;

	@Autowired
	private OpicTestCenterRepository centerRepository;

	@Autowired
	private OpicCenterReviewRepository reviewRepository;

	@MockBean
	private Geocoder geocoder;

	@BeforeEach
	void setUp() {
		reviewRepository.deleteAll();
		centerRepository.deleteAll();
		when(geocoder.geocode(anyString())).thenReturn(Geocoder.Result.found(37.5, 127.03));
	}

	@Test
	void createGeocodesTheAddress() {
		Saved saved = service.create(input("강남센터", ADDRESS, null, null));

		OpicTestCenter center = centerRepository.findById(saved.center().getId()).orElseThrow();
		assertThat(center.getLatitude()).isEqualTo(37.5);
		assertThat(center.getLongitude()).isEqualTo(127.03);
		assertThat(center.isActive()).isTrue();
		assertThat(center.getRegionName()).isEqualTo("서울");
		assertThat(saved.warning()).isNull();
		verify(geocoder).geocode(ADDRESS);
	}

	@Test
	void coordinatesGivenByTheAdminWinAndSkipGeocoding() {
		Saved saved = service.create(input("강남센터", ADDRESS, 37.1, 127.1));

		assertThat(saved.center().getLatitude()).isEqualTo(37.1);
		verify(geocoder, never()).geocode(anyString());
	}

	@Test
	void geocodingFailureStillSavesWithAWarning() {
		when(geocoder.geocode(anyString())).thenReturn(Geocoder.Result.failed("키가 없어요"));

		Saved saved = service.create(input("강남센터", ADDRESS, null, null));

		assertThat(centerRepository.findById(saved.center().getId())).isPresent();
		assertThat(saved.center().getLatitude()).isNull();
		assertThat(saved.warning()).isEqualTo("키가 없어요");
	}

	@Test
	void updateGeocodesAgainOnlyWhenTheAddressChanges() {
		UUID id = service.create(input("강남센터", ADDRESS, null, null)).center().getId();

		service.update(id, new CenterInput("강남센터 2층", " 서울  강남구 테헤란로 123 ", "02-000-0000", null, null, "서울", null, null));
		verify(geocoder, times(1)).geocode(anyString());

		when(geocoder.geocode(anyString())).thenReturn(Geocoder.Result.found(37.6, 127.0));
		service.update(id, input("강남센터 2층", "서울 서초구 강남대로 1", null, null));

		OpicTestCenter center = centerRepository.findById(id).orElseThrow();
		assertThat(center.getLatitude()).isEqualTo(37.6);
		assertThat(center.getAddress()).isEqualTo("서울 서초구 강남대로 1");
		verify(geocoder, times(2)).geocode(anyString());
	}

	@Test
	void sameNameAndAddressIsADuplicate() {
		service.create(input("강남센터", ADDRESS, null, null));

		assertThatThrownBy(() -> service.create(input(" 강남센터 ", "서울 강남구  테헤란로 123", null, null)))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.OPIC_CENTER_DUPLICATE);
	}

	@Test
	void updateCannotTakeAnotherCentersNameAndAddress() {
		service.create(input("강남센터", ADDRESS, null, null));
		UUID other = service.create(input("종로센터", "서울 종로구 1", null, null)).center().getId();

		assertThatThrownBy(() -> service.update(other, input("강남센터", ADDRESS, null, null)))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.OPIC_CENTER_DUPLICATE);
	}

	@Test
	void halfACoordinatePairIsRejected() {
		assertThatThrownBy(() -> service.create(input("강남센터", ADDRESS, 37.0, null)))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
		assertThat(centerRepository.count()).isZero();
	}

	@Test
	void deactivatedCentersLeaveThePublicList() {
		UUID id = service.create(input("강남센터", ADDRESS, null, null)).center().getId();

		service.setActive(id, false);
		assertThat(centerRepository.findByActiveTrueOrderByRegionNameAscNameAsc()).isEmpty();
		assertThat(service.findAll()).hasSize(1);

		service.setActive(id, true);
		assertThat(centerRepository.findByActiveTrueOrderByRegionNameAscNameAsc()).hasSize(1);
	}

	@Test
	void unknownCenterIsNotFound() {
		assertThatThrownBy(() -> service.setActive(UUID.randomUUID(), true))
			.isInstanceOf(ApiException.class)
			.extracting(e -> ((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.OPIC_CENTER_NOT_FOUND);
	}

	private static CenterInput input(String name, String address, Double latitude, Double longitude) {
		return new CenterInput(name, address, null, null, null, "서울", latitude, longitude);
	}
}
