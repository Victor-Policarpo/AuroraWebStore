package com.webstore.commerce.integration.igdb;

import com.webstore.commerce.config.IgdbProperties;
import com.webstore.commerce.exception.errors.ExternalApiAuthenticationException;
import com.webstore.commerce.exception.errors.ExternalApiException;
import com.webstore.commerce.exception.errors.ExternalApiRateLimitException;
import com.webstore.commerce.integration.ExternalApiCallExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class IgdbAuthServiceTest {

    private static final String AUTH_URL = "https://id.twitch.tv/oauth2/token";

    private MockRestServiceServer server;
    private IgdbAuthService authService;

    @BeforeEach
    void setUp() {
        authService = newAuthService();
    }

    private IgdbAuthService newAuthService() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        IgdbProperties properties = new IgdbProperties();
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");
        properties.getApi().setBaseUrl("https://api.igdb.com/v4");
        properties.getAuth().setUrl(AUTH_URL);

        ExternalApiCallExecutor executor = new ExternalApiCallExecutor(millis -> { }, new Random(0));
        return new IgdbAuthService(restClient, executor, properties);
    }

    @Test
    @DisplayName("Should obtain an access token from the OAuth service")
    void obtainsToken() {
        expectToken("abc", 3600);

        assertEquals("abc", authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should reuse a cached valid token")
    void reusesCachedToken() {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenBody("abc", 3600), MediaType.APPLICATION_JSON));

        assertEquals("abc", authService.getAccessToken());
        assertEquals("abc", authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should renew tokens that are too short lived to be safely cached")
    void renewsShortLivedTokens() {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withSuccess(tokenBody("first", 1), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withSuccess(tokenBody("second", 3600), MediaType.APPLICATION_JSON));

        assertEquals("first", authService.getAccessToken());
        assertEquals("second", authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should renew the token after it is invalidated")
    void renewsAfterInvalidation() {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withSuccess(tokenBody("first", 3600), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withSuccess(tokenBody("second", 3600), MediaType.APPLICATION_JSON));

        assertEquals("first", authService.getAccessToken());
        authService.invalidateToken();
        assertEquals("second", authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should reject a missing access token")
    void rejectsMissingAccessToken() {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withSuccess("{\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        assertThrows(ExternalApiException.class, () -> authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should reject a blank access token")
    void rejectsBlankAccessToken() {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withSuccess(tokenBody("", 3600), MediaType.APPLICATION_JSON));

        assertThrows(ExternalApiException.class, () -> authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should reject a missing expires_in")
    void rejectsMissingExpiresIn() {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withSuccess("{\"access_token\":\"abc\"}", MediaType.APPLICATION_JSON));

        assertThrows(ExternalApiException.class, () -> authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should reject a non positive expires_in")
    void rejectsNonPositiveExpiresIn() {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withSuccess(tokenBody("abc", 0), MediaType.APPLICATION_JSON));

        assertThrows(ExternalApiException.class, () -> authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should map OAuth 400 responses to an external authentication failure")
    void mapsBadRequestToAuthenticationFailure() {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withBadRequest());

        assertThrows(ExternalApiAuthenticationException.class, () -> authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should map OAuth 401 responses to an external authentication failure")
    void mapsUnauthorizedToAuthenticationFailure() {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withUnauthorizedRequest());

        assertThrows(ExternalApiAuthenticationException.class, () -> authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should map OAuth 403 responses to an external authentication failure")
    void mapsForbiddenToAuthenticationFailure() {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withForbiddenRequest());

        assertThrows(ExternalApiAuthenticationException.class, () -> authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should retry OAuth 5xx responses and then fail")
    void retriesServerErrors() {
        server.expect(ExpectedCount.times(3), requestTo(AUTH_URL))
                .andRespond(withServerError());

        assertThrows(ExternalApiException.class, () -> authService.getAccessToken());
        server.verify();
    }

    @Test
    @DisplayName("Should map OAuth 429 responses to a rate limit failure")
    void mapsTooManyRequests() {
        server.expect(ExpectedCount.times(3), requestTo(AUTH_URL))
                .andRespond(withTooManyRequests(1));

        ExternalApiRateLimitException exception = assertThrows(ExternalApiRateLimitException.class,
                () -> authService.getAccessToken());

        assertEquals(1L, exception.getRetryAfterSeconds());
        server.verify();
    }

    @Test
    @DisplayName("Should not renew the token multiple times on concurrent calls")
    void doesNotRenewConcurrently() throws Exception {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withSuccess(tokenBody("abc", 3600), MediaType.APPLICATION_JSON));

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return authService.getAccessToken();
                }));
            }
            start.countDown();

            for (Future<String> future : futures) {
                assertEquals("abc", future.get(5, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        server.verify();
    }

    @Test
    @DisplayName("Should allow concurrent invalidation without losing consistency")
    void invalidatesSafely() throws Exception {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withSuccess(tokenBody("first", 3600), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andRespond(withSuccess(tokenBody("second", 3600), MediaType.APPLICATION_JSON));

        assertEquals("first", authService.getAccessToken());

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                futures.add(pool.submit(authService::invalidateToken));
            }
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals("second", authService.getAccessToken());
        server.verify();
    }

    private void expectToken(String token, long expiresIn) {
        server.expect(ExpectedCount.once(), requestTo(AUTH_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenBody(token, expiresIn), MediaType.APPLICATION_JSON));
    }

    private static String tokenBody(String token, long expiresIn) {
        return "{\"access_token\":\"" + token + "\",\"expires_in\":" + expiresIn
                + ",\"token_type\":\"bearer\"}";
    }
}
