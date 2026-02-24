package com.opicer.api.auth;

import com.opicer.api.config.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

	private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

	private final AuthProperties authProperties;

	public OAuth2LoginFailureHandler(AuthProperties authProperties) {
		this.authProperties = authProperties;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception) throws IOException {

		String reason = "unknown";
		if (exception instanceof OAuth2AuthenticationException ex) {
			reason = ex.getError().getErrorCode();
			log.error("[OAuth2] Login failed — errorCode={}, description={}, uri={}",
				ex.getError().getErrorCode(),
				ex.getError().getDescription(),
				ex.getError().getUri());
		} else {
			log.error("[OAuth2] Login failed — {}", exception.getMessage(), exception);
		}

		response.sendRedirect(authProperties.getFrontendBaseUrl() + "/auth/error?reason=" + reason);
	}
}
