package com.catalogue.verg.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for auth_service. Creates the corresponding auth user for an identity that this
 * catalogue is onboarding, so the auth layer can issue credentials for it.
 */
@Slf4j
@Service
public class AuthUserService {

    static final String AUTH_USER_CREATE_PATH = "/auth/v1/auth_user_create";

    /** Header auth_service expects the api key on; change here if that contract changes. */
    static final String API_KEY_HEADER = "apiKey";

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${auth.service.api.key}")
    private String authServiceApiKey;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * POSTs the given user details to {@code {auth.service.url}/auth/v1/auth_user_create}.
     * <p>
     * The full {@link ResponseEntity} is returned so callers can gate on the status code before
     * continuing. A 4xx/5xx from auth_service surfaces as {@code HttpClientErrorException} /
     * {@code HttpServerErrorException}, and an unreachable auth_service as
     * {@code ResourceAccessException} — none of them are swallowed here.
     */
    public ResponseEntity<Map<String, Object>> createAuthUser(String firstName, String lastName, String email,
                                                              String userId, String orgId, String entityType) {
        String uri = buildUri();

        Map<String, Object> request = new HashMap<>();
        request.put("firstName", firstName);
        request.put("lastName", lastName);
        request.put("email", email);
        request.put("userId", userId);
        request.put("orgId", orgId);
        request.put("entityType", entityType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authServiceApiKey != null && !authServiceApiKey.isBlank()) {
            headers.set(API_KEY_HEADER, authServiceApiKey.trim());
        } else {
            log.warn("AuthUserService::createAuthUser::no api key configured, calling auth_service without the {} header",
                    API_KEY_HEADER);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        log.info("AuthUserService::createAuthUser::posting to {} for userId: {}, orgId: {}, entityType: {}",
                uri, userId, orgId, entityType);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(uri, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        log.info("AuthUserService::createAuthUser::auth_service responded with status: {} for userId: {}",
                response.getStatusCode(), userId);
        return response;
    }

    private String buildUri() {
        if (authServiceUrl == null || authServiceUrl.isBlank()) {
            throw new IllegalStateException("AuthUserService::auth.service.url is not configured");
        }
        String base = authServiceUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + AUTH_USER_CREATE_PATH;
    }
}
