package com.opicer.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.opicer.api.auth.JwtAuthenticationFilter;
import com.opicer.api.auth.OAuth2LoginSuccessHandler;

@Configuration
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
		OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**"));
		http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/h2-console/**", "/actuator/health", "/api/health").permitAll()
			.requestMatchers("/oauth2/**", "/login/**").permitAll()
			.requestMatchers("/api/auth/login/**", "/api/auth/logout").permitAll()
			.requestMatchers("/api/admin/**").hasRole("ADMIN")
			.requestMatchers("/api/auth/me").authenticated()
			.requestMatchers("/api/**").authenticated()
			.anyRequest().permitAll()
		);
		http.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler));
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
