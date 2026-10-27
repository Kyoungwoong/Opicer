package com.opicer.api.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/**
 * Stops a dev-profile start when application-dev.yaml's required environment variables are missing. Without this,
 * Spring keeps an unresolved {@code ${X}} as literal text, so a missing secret only fails later and confusingly.
 * Registered in META-INF/spring.factories so it runs before any bean is created.
 */
public class RequiredDevSettings implements EnvironmentPostProcessor {

	static final List<String> REQUIRED = List.of(
		"spring.datasource.url",
		"spring.datasource.username",
		"spring.datasource.password",
		"spring.security.oauth2.client.registration.kakao.client-id",
		"spring.security.oauth2.client.registration.kakao.client-secret",
		"spring.security.oauth2.client.registration.kakao.redirect-uri",
		"opicer.auth.jwt-secret",
		"opicer.auth.frontend-base-url",
		"opicer.ai.openai-api-key");

	private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}:]+)}");

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!environment.acceptsProfiles(Profiles.of("dev"))) {
			return;
		}
		List<String> missing = missingVariables(environment);
		if (!missing.isEmpty()) {
			throw new IllegalStateException("The dev profile needs these environment variables: " + String.join(", ", missing));
		}
	}

	/** Environment variable names behind the required settings that do not resolve. */
	static List<String> missingVariables(ConfigurableEnvironment environment) {
		List<String> missing = new ArrayList<>();
		for (String key : REQUIRED) {
			String raw = rawValue(environment, key);
			try {
				String value = environment.getProperty(key);
				if (value == null || value.isBlank()) {
					missing.add(variableName(raw, key));
				}
			} catch (IllegalArgumentException unresolved) {
				missing.add(variableName(raw, key));
			}
		}
		return missing;
	}

	private static String rawValue(ConfigurableEnvironment environment, String key) {
		for (var source : environment.getPropertySources()) {
			Object value = source.getProperty(key);
			if (value != null) {
				return value.toString();
			}
		}
		return null;
	}

	private static String variableName(String raw, String key) {
		if (raw != null) {
			Matcher matcher = PLACEHOLDER.matcher(raw);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}
		return key;
	}
}
