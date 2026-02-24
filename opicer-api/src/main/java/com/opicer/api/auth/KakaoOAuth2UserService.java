package com.opicer.api.auth;

import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KakaoOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private static final Logger log = LoggerFactory.getLogger(KakaoOAuth2UserService.class);

	private final DefaultOAuth2UserService delegate;

	public KakaoOAuth2UserService() {
		DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

		// Kakao's user-info endpoint returns "application/json;charset=utf-8"
		// which Spring's default converter may reject — accept all media types explicitly
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().stream()
			.filter(c -> c instanceof MappingJackson2HttpMessageConverter)
			.forEach(c -> ((MappingJackson2HttpMessageConverter) c)
				.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL)));
		delegate.setRestOperations(restTemplate);

		this.delegate = delegate;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		log.debug("[Kakao] Requesting user info from: {}",
			userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
		try {
			OAuth2User user = delegate.loadUser(userRequest);
			log.debug("[Kakao] User info loaded — attribute keys: {}", user.getAttributes().keySet());
			return user;
		} catch (OAuth2AuthenticationException ex) {
			log.error("[Kakao] User info failed — errorCode={}, description={}",
				ex.getError().getErrorCode(),
				ex.getError().getDescription(),
				ex);
			throw ex;
		}
	}
}
