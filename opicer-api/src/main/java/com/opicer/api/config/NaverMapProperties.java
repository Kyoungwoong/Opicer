package com.opicer.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * NAVER Cloud Platform Maps credentials for server-side geocoding. Without a client id and secret, centers are
 * saved without coordinates and admins can enter them by hand.
 */
@ConfigurationProperties(prefix = "opicer.naver-map")
public class NaverMapProperties {

	private String clientId = "";
	private String clientSecret = "";
	/** Kept configurable so an endpoint change on NAVER's side needs no code change. */
	private String geocodeUrl = "https://maps.apigw.ntruss.com/map-geocode/v2/geocode";
	private int timeoutMs = 3000;

	public boolean isConfigured() {
		return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
	}

	public String getClientId() { return clientId; }
	public void setClientId(String clientId) { this.clientId = clientId; }
	public String getClientSecret() { return clientSecret; }
	public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
	public String getGeocodeUrl() { return geocodeUrl; }
	public void setGeocodeUrl(String geocodeUrl) { this.geocodeUrl = geocodeUrl; }
	public int getTimeoutMs() { return timeoutMs; }
	public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}
