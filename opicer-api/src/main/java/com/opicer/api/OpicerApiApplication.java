package com.opicer.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OpicerApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpicerApiApplication.class, args);
	}

}
