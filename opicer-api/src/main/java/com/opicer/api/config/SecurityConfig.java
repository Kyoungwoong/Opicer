package com.opicer.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.http.HttpStatus;

import com.opicer.api.auth.JwtAuthenticationFilter;
import com.opicer.api.auth.KakaoOAuth2UserService;
import com.opicer.api.auth.OAuth2LoginFailureHandler;
import com.opicer.api.auth.OAuth2LoginSuccessHandler;

@Configuration
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
	private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
	private final KakaoOAuth2UserService kakaoOAuth2UserService;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
		OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
		OAuth2LoginFailureHandler oAuth2LoginFailureHandler,
		KakaoOAuth2UserService kakaoOAuth2UserService) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
		this.oAuth2LoginFailureHandler = oAuth2LoginFailureHandler;
		this.kakaoOAuth2UserService = kakaoOAuth2UserService;
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
		http.oauth2Login(oauth2 -> oauth2
			.successHandler(oAuth2LoginSuccessHandler)
			.failureHandler(oAuth2LoginFailureHandler)
			.userInfoEndpoint(userInfo -> userInfo.userService(kakaoOAuth2UserService)));
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
		http.exceptionHandling(handler -> handler
			.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
			.accessDeniedHandler(new AccessDeniedHandlerImpl())
		);
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
}
