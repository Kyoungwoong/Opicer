package com.opicer.api.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final StorageProperties storageProperties;

	public WebConfig(StorageProperties storageProperties) {
		this.storageProperties = storageProperties;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String prefix = storageProperties.getGoodAnswerUrlPrefix();
		String directory = storageProperties.getGoodAnswerDir();
		String location = Path.of(directory).toAbsolutePath().normalize().toUri().toString();
		registry.addResourceHandler(prefix + "/**")
			.addResourceLocations(location);
	}
}
