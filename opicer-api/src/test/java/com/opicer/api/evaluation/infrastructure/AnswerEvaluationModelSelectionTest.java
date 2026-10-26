package com.opicer.api.evaluation.infrastructure;

import com.opicer.api.config.AiProperties;
import com.opicer.api.config.EvaluationProperties;
import com.opicer.api.evaluation.application.AnswerEvaluationModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;

/** The evaluation provider is chosen by configuration; OpenAI is the default. */
class AnswerEvaluationModelSelectionTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withUserConfiguration(Providers.class);

	@Test
	void defaultsToOpenAi() {
		runner.run(context -> assertThat(context.getBean(AnswerEvaluationModel.class))
			.isInstanceOf(OpenAiAnswerEvaluationModel.class));
	}

	@Test
	void modelIdComesFromConfiguration() {
		runner.withPropertyValues("opicer.ai.evaluation.openai-model=some-other-model")
			.run(context -> assertThat(context.getBean(AnswerEvaluationModel.class).model()).isEqualTo("some-other-model"));
	}

	@Test
	void unknownProviderLeavesNoImplementation() {
		runner.withPropertyValues(EvaluationProperties.PROVIDER_KEY + "=other")
			.run(context -> assertThat(context.getBeansOfType(AnswerEvaluationModel.class)).isEmpty());
	}

	@Configuration
	@EnableConfigurationProperties({AiProperties.class, EvaluationProperties.class})
	@Import(OpenAiAnswerEvaluationModel.class)
	static class Providers {
		@Bean
		RestClient.Builder restClientBuilder() {
			return RestClient.builder();
		}
	}
}
