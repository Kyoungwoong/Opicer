package com.opicer.api.opic.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opicer.api.config.NaverMapProperties;
import com.opicer.api.opic.application.Geocoder;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** NAVER Cloud Platform Maps geocoding: {@code x} is longitude and {@code y} latitude, both as strings. */
@Component
public class NaverGeocoder implements Geocoder {

	private static final Logger log = LoggerFactory.getLogger(NaverGeocoder.class);

	private final NaverMapProperties properties;
	private final RestClient restClient;

	public NaverGeocoder(NaverMapProperties properties, RestClient.Builder builder) {
		this.properties = properties;
		// A non-positive timeout keeps the builder's own factory (tests bind a mock server to it).
		if (properties.getTimeoutMs() > 0) {
			JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(Duration.ofMillis(properties.getTimeoutMs())).build());
			requestFactory.setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()));
			builder.requestFactory(requestFactory);
		}
		this.restClient = builder.build();
	}

	@Override
	public Result geocode(String address) {
		if (!properties.isConfigured()) {
			return Result.failed("네이버 지도 키가 설정되지 않아 좌표를 찾지 않았어요.");
		}
		try {
			GeocodeResponse response = restClient.get()
				.uri(properties.getGeocodeUrl() + "?query={query}", address)
				.header("x-ncp-apigw-api-key-id", properties.getClientId())
				.header("x-ncp-apigw-api-key", properties.getClientSecret())
				.retrieve()
				.body(GeocodeResponse.class);
			if (response == null || response.addresses() == null || response.addresses().isEmpty()) {
				return Result.failed("주소로 좌표를 찾지 못했어요. 도로명 주소를 확인하거나 좌표를 직접 입력하세요.");
			}
			GeocodeAddress first = response.addresses().get(0);
			return Result.found(Double.parseDouble(first.y()), Double.parseDouble(first.x()));
		} catch (RestClientException | NumberFormatException | NullPointerException e) {
			log.warn("[Geocode] failed for an address: {}", e.getMessage());
			return Result.failed("좌표 변환 서비스에 연결하지 못했어요. 잠시 후 다시 저장하거나 좌표를 직접 입력하세요.");
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record GeocodeResponse(String status, List<GeocodeAddress> addresses) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record GeocodeAddress(String roadAddress, String x, String y) {}
}
