package ru.dpoliwhi.authservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAuthClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.server-url:http://localhost:8282}")
    private String serverUrl;

    @Value("${keycloak.realm:dpoliwhi-realm}")
    private String realm;

    @Value("${keycloak.client-id:backend}")
    private String clientId;

    @Value("${keycloak.client-secret:my_secret}")
    private String clientSecret;

    public AccessTokenResponse login(String username, String password) {
        log.debug("Requesting tokens for user: {}", username);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", username);
        formData.add("password", password);

        return requestTokens(formData);
    }

    public AccessTokenResponse refreshToken(String refreshToken) {
        log.debug("Refreshing access token");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", refreshToken);

        return requestTokens(formData);
    }

    private AccessTokenResponse requestTokens(MultiValueMap<String, String> formData) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<AccessTokenResponse> response = restTemplate.postForEntity(
                getTokenEndpoint(),
                request,
                AccessTokenResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to obtain tokens from Keycloak");
        }

        return response.getBody();
    }

    private String getTokenEndpoint() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }
}
