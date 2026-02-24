package com.opicer.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
		http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/h2-console/**", "/actuator/health").permitAll()
			.anyRequest().authenticated()
		);
		http.httpBasic(Customizer.withDefaults());
		return http.build();
	}
}
