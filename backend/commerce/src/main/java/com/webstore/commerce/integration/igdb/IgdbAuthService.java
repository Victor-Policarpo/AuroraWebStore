package com.webstore.commerce.integration.igdb;

import com.webstore.commerce.integration.igdb.dto.IgdbTokenResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class IgdbAuthService {

    private final RestClient twitchAuthRestClient;
    private final String clientId;
    private final String clientSecret;
    private final String authUrl;

    private String accessToken;
    private long tokenExpiresAt;

    public IgdbAuthService(
            @Qualifier("twitchAuthRestClient")
            RestClient twitchAuthRestClient,
            @Value("${igdb.client-id}") String clientId,
            @Value("${igdb.client-secret}") String clientSecret,
            @Value("${igdb.auth.url}") String authUrl
    ) {
        this.twitchAuthRestClient = twitchAuthRestClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authUrl = authUrl;
    }

    public synchronized String getAccessToken() {
        if (accessToken != null
                && System.currentTimeMillis() < tokenExpiresAt) {
            return accessToken;
        }

        MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "client_credentials");

        IgdbTokenResponse response = twitchAuthRestClient.post()
                .uri(authUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(IgdbTokenResponse.class);

        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()) {
            throw new IllegalStateException(
                    "Failed to obtain IGDB access token"
            );
        }

        accessToken = response.accessToken();

        long safetyMarginMillis = 60_000L;
        tokenExpiresAt = System.currentTimeMillis()
                + (response.expiresIn() * 1000L)
                - safetyMarginMillis;

        return accessToken;
    }
}