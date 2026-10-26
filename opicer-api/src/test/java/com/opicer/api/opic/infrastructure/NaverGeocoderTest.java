package com.opicer.api.opic.infrastructure;

import com.opicer.api.config.NaverMapProperties;
import com.opicer.api.opic.application.Geocoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Contract test against a fake HTTP server; no real NAVER call. */
class NaverGeocoderTest {

	private final NaverMapProperties properties = new NaverMapProperties();
	private MockRestServiceServer server;
	private NaverGeocoder geocoder;

	@BeforeEach
	void setUp() {
		properties.setClientId("test-id");
		properties.setClientSecret("test-secret");
		properties.setGeocodeUrl("https://geocode.test/map-geocode/v2/geocode");
		properties.setTimeoutMs(0);
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		geocoder = new NaverGeocoder(properties, builder);
	}

	@Test
	void sendsKeysAndReadsLongitudeAsXAndLatitudeAsY() {
		server.expect(requestTo(containsString("https://geocode.test/map-geocode/v2/geocode?query=")))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("x-ncp-apigw-api-key-id", "test-id"))
			.andExpect(header("x-ncp-apigw-api-key", "test-secret"))
			.andRespond(withSuccess("""
				{"status":"OK","meta":{"totalCount":1},
				 "addresses":[{"roadAddress":"서울특별시 강남구 테헤란로 123","x":"127.0330","y":"37.5000"}]}
				""", MediaType.APPLICATION_JSON));

		Geocoder.Result result = geocoder.geocode("서울 강남구 테헤란로 123");

		assertThat(result.isFound()).isTrue();
		assertThat(result.latitude()).isEqualTo(37.5);
		assertThat(result.longitude()).isEqualTo(127.033);
		server.verify();
	}

	@Test
	void noMatchIsAFailureWithAReason() {
		server.expect(requestTo(containsString("query=")))
			.andRespond(withSuccess("{\"status\":\"OK\",\"addresses\":[]}", MediaType.APPLICATION_JSON));

		Geocoder.Result result = geocoder.geocode("없는 주소");

		assertThat(result.isFound()).isFalse();
		assertThat(result.latitude()).isNull();
		assertThat(result.failure()).contains("좌표를 찾지 못했어요");
	}

	@Test
	void anAddressWithoutCoordinatesCountsAsNoMatch() {
		server.expect(requestTo(containsString("query=")))
			.andRespond(withSuccess("{\"status\":\"OK\",\"addresses\":[{\"roadAddress\":\"서울\"}]}",
				MediaType.APPLICATION_JSON));

		Geocoder.Result result = geocoder.geocode("서울");

		assertThat(result.isFound()).isFalse();
		assertThat(result.failure()).contains("좌표를 찾지 못했어요");
	}

	@Test
	void serverErrorIsAFailureNotAnException() {
		server.expect(requestTo(containsString("query="))).andRespond(withServerError());

		Geocoder.Result result = geocoder.geocode("서울 강남구 테헤란로 123");

		assertThat(result.isFound()).isFalse();
		assertThat(result.failure()).contains("연결하지 못했어요");
	}

	@Test
	void withoutKeysNothingIsCalled() {
		properties.setClientSecret("");
		server.expect(never(), requestTo(containsString("query=")));

		Geocoder.Result result = geocoder.geocode("서울 강남구 테헤란로 123");

		assertThat(result.isFound()).isFalse();
		assertThat(result.failure()).contains("키가 설정되지 않아");
		server.verify();
	}
}
