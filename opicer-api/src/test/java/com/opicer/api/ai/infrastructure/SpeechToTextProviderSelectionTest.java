package com.opicer.api.ai.infrastructure;

import com.opicer.api.ai.application.SpeechToText;
import com.opicer.api.config.AiProperties;
import com.opicer.api.config.SttProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;

/** Swapping STT is configuration only: exactly one {@link SpeechToText} bean per provider value. */
class SpeechToTextProviderSelectionTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withUserConfiguration(Providers.class);

	@Test
	void defaultsToMockWhenUnset() {
		runner.run(context -> assertThat(context.getBean(SpeechToText.class)).isInstanceOf(MockSpeechToText.class));
	}

	@Test
	void mockWhenConfigured() {
		runner.withPropertyValues(SttProperties.PROVIDER_KEY + "=mock")
			.run(context -> assertThat(context.getBean(SpeechToText.class)).isInstanceOf(MockSpeechToText.class));
	}

	@Test
	void openAiWhenConfigured() {
		runner.withPropertyValues(SttProperties.PROVIDER_KEY + "=openai")
			.run(context -> {
				assertThat(context.getBeansOfType(SpeechToText.class)).hasSize(1);
				assertThat(context.getBean(SpeechToText.class)).isInstanceOf(OpenAiWhisperSpeechToText.class);
			});
	}

	@Configuration
	@EnableConfigurationProperties({AiProperties.class, SttProperties.class})
	@Import({MockSpeechToText.class, OpenAiWhisperSpeechToText.class})
	static class Providers {
		@Bean
		RestClient.Builder restClientBuilder() {
			return RestClient.builder();
		}
	}
}
