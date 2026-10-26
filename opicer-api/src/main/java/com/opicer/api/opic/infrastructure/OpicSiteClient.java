package com.opicer.api.opic.infrastructure;

import com.opicer.api.config.OpicSyncProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Fetches public schedule/center JSON from opic.or.kr. Callers are responsible for pacing requests. */
@Component
public class OpicSiteClient {

	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

	private final RestClient restClient;
	private final OpicSiteParser parser;

	public OpicSiteClient(OpicSyncProperties properties, OpicSiteParser parser) {
		this.parser = parser;
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
			HttpClient.newBuilder().connectTimeout(Duration.ofMillis(properties.getTimeoutMs())).build()
		);
		requestFactory.setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()));
		this.restClient = RestClient.builder()
			.baseUrl(properties.getBaseUrl())
			.requestFactory(requestFactory)
			.defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
			.build();
	}

	public List<OpicSiteParser.ScheduleRow> fetchSchedules(YearMonth month) {
		String body = restClient.post()
			.uri(uri -> uri.path("/controller.opic.site.receipt.AnnualScheduleServlet")
				.queryParam("p_process", "select-new-ajax-list")
				.queryParam("p_totalMonth", MONTH.format(month))
				.build())
			.retrieve()
			.body(String.class);
		return parser.parseSchedules(body);
	}

	public List<OpicSiteParser.CenterRow> fetchCenters() {
		String body = restClient.post()
			.uri("/controller.opic.common.CommCenterMapServlet")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body("p_process=select-center-list")
			.retrieve()
			.body(String.class);
		return parser.parseCenters(body);
	}
}
