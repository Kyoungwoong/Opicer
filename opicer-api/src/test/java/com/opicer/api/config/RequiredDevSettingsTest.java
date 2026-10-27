package com.opicer.api.config;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/** A dev start without its secrets stops up front and names every missing variable. */
class RequiredDevSettingsTest {

	private final RequiredDevSettings check = new RequiredDevSettings();

	@Test
	void devWithMissingVariablesFailsAndNamesThem() {
		StandardEnvironment environment = environment(Map.of(
			"spring.datasource.url", "${OPICER_TEST_MISSING_DB_URL}",
			"opicer.auth.jwt-secret", "${OPICER_TEST_MISSING_JWT}"));
		environment.setActiveProfiles("dev");

		assertThatThrownBy(() -> check.postProcessEnvironment(environment, new SpringApplication()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("OPICER_TEST_MISSING_DB_URL")
			.hasMessageContaining("OPICER_TEST_MISSING_JWT");
	}

	@Test
	void devWithEverySettingPresentStarts() {
		Map<String, Object> values = new HashMap<>();
		RequiredDevSettings.REQUIRED.forEach(key -> values.put(key, "set"));
		StandardEnvironment environment = environment(values);
		environment.setActiveProfiles("dev");

		assertThatNoException().isThrownBy(() -> check.postProcessEnvironment(environment, new SpringApplication()));
		assertThat(RequiredDevSettings.missingVariables(environment)).isEmpty();
	}

	@Test
	void otherProfilesAreNotChecked() {
		StandardEnvironment environment = environment(Map.of("spring.datasource.url", "${OPICER_TEST_MISSING_DB_URL}"));
		environment.setActiveProfiles("local");

		assertThatNoException().isThrownBy(() -> check.postProcessEnvironment(environment, new SpringApplication()));
	}

	private static StandardEnvironment environment(Map<String, Object> values) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("test", new HashMap<>(values)));
		return environment;
	}
}
