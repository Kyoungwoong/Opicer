package com.opicer.api.ai.infrastructure;

import com.opicer.api.config.SttProperties;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import static org.assertj.core.api.Assertions.assertThat;

/** Guards the profile defaults: local is the default profile with mock STT, dev uses real STT and needs its secrets. */
class SttProfileDefaultsTest {

	@Test
	void localIsTheDefaultProfileAndUsesMockStt() {
		assertThat(load("application.yaml").getProperty("spring.profiles.default")).isEqualTo("local");
		assertThat(load("application-local.yaml").getProperty(SttProperties.PROVIDER_KEY))
			.isEqualTo("${OPICER_STT_PROVIDER:mock}");
	}

	@Test
	void devUsesOpenAiSttAndHasNoDefaultsForSecrets() {
		Properties dev = load("application-dev.yaml");
		assertThat(dev.getProperty(SttProperties.PROVIDER_KEY)).isEqualTo("${OPICER_STT_PROVIDER:openai}");
		assertThat(dev.getProperty("spring.datasource.url")).isEqualTo("${OPICER_DB_URL}");
		assertThat(dev.getProperty("spring.datasource.password")).isEqualTo("${OPICER_DB_PASSWORD}");
		assertThat(dev.getProperty("opicer.auth.jwt-secret")).isEqualTo("${JWT_SECRET}");
		assertThat(dev.getProperty("opicer.ai.openai-api-key")).isEqualTo("${OPENAI_API_KEY}");
	}

	private static Properties load(String file) {
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
		// Read the main resources directly: the test classpath has its own application.yaml.
		yaml.setResources(new FileSystemResource("src/main/resources/" + file));
		return yaml.getObject();
	}
}
