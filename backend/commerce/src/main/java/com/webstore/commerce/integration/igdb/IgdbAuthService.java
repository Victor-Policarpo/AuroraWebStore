package com.webstore.commerce.integration.igdb;

import com.webstore.commerce.config.IgdbProperties;
import com.webstore.commerce.exception.errors.ExternalApiAuthenticationException;
import com.webstore.commerce.exception.errors.ExternalApiException;
import com.webstore.commerce.integration.ExternalApiCallExecutor;
import com.webstore.commerce.integration.ExternalApiErrors;
import com.webstore.commerce.integration.igdb.dto.IgdbTokenResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class IgdbAuthService {

    private static final long MIN_SAFETY_MARGIN_MILLIS = 30_000L;
    private static final long SAFETY_MARGIN_PERCENT = 15L;

    private final RestClient twitchAuthRestClient;
    private final ExternalApiCallExecutor callExecutor;
    private final String clientId;
    private final String clientSecret;
    private final String authUrl;

    private volatile String accessToken;
    private volatile long tokenExpiresAt;

    public IgdbAuthService(
            @Qualifier("twitchAuthRestClient") RestClient twitchAuthRestClient,
            ExternalApiCallExecutor callExecutor,
            IgdbProperties properties
    ) {
        this.twitchAuthRestClient = twitchAuthRestClient;
        this.callExecutor = callExecutor;
        this.clientId = properties.getClientId();
        this.clientSecret = properties.getClientSecret();
        this.authUrl = properties.getAuth().getUrl();
    }

    /**
     * Returns a cached, still valid token or obtains a new one. Synchronized so
     * concurrent callers cannot trigger duplicated renewals.
     */
    public synchronized String getAccessToken() {
        String cached = accessToken;
        if (cached != null && System.currentTimeMillis() < tokenExpiresAt) {
            return cached;
        }

        return refreshToken();
    }

    /**
     * Discards the cached token so the next call obtains a fresh one.
     */
    public synchronized void invalidateToken() {
        this.accessToken = null;
        this.tokenExpiresAt = 0L;
    }

    private String refreshToken() {
        IgdbTokenResponse response;

        try {
            response = callExecutor.execute("Twitch OAuth token", this::requestToken);
        } catch (RestClientResponseException e) {
            throw mapOAuthError(e);
        } catch (ResourceAccessException e) {
            throw new ExternalApiException("Unable to reach the Twitch OAuth service", e);
        } catch (RestClientException e) {
            throw new ExternalApiException("Unexpected error while requesting an IGDB access token", e);
        }

        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()) {
            throw new ExternalApiException("The Twitch OAuth service did not return a valid access token");
        }

        if (response.expiresIn() <= 0) {
            throw new ExternalApiException("The Twitch OAuth service did not return a valid token lifetime");
        }

        long lifetimeMillis;
        try {
            lifetimeMillis = Math.multiplyExact(response.expiresIn(), 1000L);
        } catch (ArithmeticException e) {
            throw new ExternalApiException("The Twitch OAuth service returned an invalid token lifetime", e);
        }

        long safetyMarginMillis = Math.max(
                MIN_SAFETY_MARGIN_MILLIS,
                lifetimeMillis * SAFETY_MARGIN_PERCENT / 100L);

        this.accessToken = response.accessToken();
        this.tokenExpiresAt = System.currentTimeMillis() + lifetimeMillis - safetyMarginMillis;

        return this.accessToken;
    }

    private IgdbTokenResponse requestToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "client_credentials");

        return twitchAuthRestClient.post()
                .uri(authUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(IgdbTokenResponse.class);
    }

    private ExternalApiException mapOAuthError(RestClientResponseException e) {
        int status = e.getStatusCode().value();

        if (status == 400 || status == 401 || status == 403) {
            return new ExternalApiAuthenticationException(
                    "The Twitch OAuth service rejected the provided credentials", e);
        }

        return ExternalApiErrors.map("Twitch OAuth", e);
    }
}
