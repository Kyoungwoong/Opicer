package com.opicer.api.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opicer.auth")
public class AuthProperties {

	private String jwtSecret;
	private long jwtTtlSeconds = 3600;
	private String cookieName = "OPICER_AUTH";
	private boolean cookieSecure = false;
	private List<String> adminAllowlist = new ArrayList<>();
	private String frontendBaseUrl = "http://localhost:3000";

	public String getJwtSecret() {
		return jwtSecret;
	}

	public void setJwtSecret(String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	public long getJwtTtlSeconds() {
		return jwtTtlSeconds;
	}

	public void setJwtTtlSeconds(long jwtTtlSeconds) {
		this.jwtTtlSeconds = jwtTtlSeconds;
	}

	public String getCookieName() {
		return cookieName;
	}

	public void setCookieName(String cookieName) {
		this.cookieName = cookieName;
	}

	public boolean isCookieSecure() {
		return cookieSecure;
	}

	public void setCookieSecure(boolean cookieSecure) {
		this.cookieSecure = cookieSecure;
	}

	public List<String> getAdminAllowlist() {
		return adminAllowlist;
	}

	public void setAdminAllowlist(List<String> adminAllowlist) {
		this.adminAllowlist = adminAllowlist;
	}

	public String getFrontendBaseUrl() {
		return frontendBaseUrl;
	}

	public void setFrontendBaseUrl(String frontendBaseUrl) {
		this.frontendBaseUrl = frontendBaseUrl;
	}
}
