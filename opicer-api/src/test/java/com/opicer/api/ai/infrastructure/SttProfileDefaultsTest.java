package com.opicer.api.ai.infrastructure;

import com.opicer.api.config.SttProperties;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import static org.assertj.core.api.Assertions.assertThat;

/** Guards the profile defaults: mock locally, real STT in the production profile. */
class SttProfileDefaultsTest {

	@Test
	void localDefaultsToMockAndProductionDefaultsToOpenAi() {
		assertThat(load("application.yaml").getProperty(SttProperties.PROVIDER_KEY))
			.isEqualTo("${OPICER_STT_PROVIDER:mock}");
		assertThat(load("application-postgres.yaml").getProperty(SttProperties.PROVIDER_KEY))
			.isEqualTo("${OPICER_STT_PROVIDER:openai}");
	}

	private static Properties load(String file) {
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
		// Read the main resources directly: the test classpath has its own application.yaml.
		yaml.setResources(new FileSystemResource("src/main/resources/" + file));
		return yaml.getObject();
	}
}
