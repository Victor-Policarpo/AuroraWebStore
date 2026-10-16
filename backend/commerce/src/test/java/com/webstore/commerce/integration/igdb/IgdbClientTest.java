package com.webstore.commerce.integration.igdb;

import com.webstore.commerce.config.IgdbProperties;
import com.webstore.commerce.exception.errors.ExternalApiAuthenticationException;
import com.webstore.commerce.exception.errors.ExternalApiException;
import com.webstore.commerce.exception.errors.ExternalApiRateLimitException;
import com.webstore.commerce.integration.ExternalApiCallExecutor;
import com.webstore.commerce.integration.igdb.dto.IgdbGameResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Random;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class IgdbClientTest {

    private static final String BASE_URL = "https://api.igdb.com/v4";
    private static final String GAMES_URL = BASE_URL + "/games";

    private MockRestServiceServer server;
    private IgdbAuthService authService;
    private IgdbClient client;

    @BeforeEach
    void setUp() {
        authService = mock(IgdbAuthService.class);
        lenient().when(authService.getAccessToken()).thenReturn("token");
        client = clientWithLimit(10);
    }

    private IgdbClient clientWithLimit(int limit) {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        IgdbProperties properties = new IgdbProperties();
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");
        properties.getApi().setBaseUrl(BASE_URL);
        properties.getAuth().setUrl("https://id.twitch.tv/oauth2/token");
        properties.getSearch().setLimit(limit);

        ExternalApiCallExecutor executor = new ExternalApiCallExecutor(millis -> { }, new Random(0));
        return new IgdbClient(restClient, authService, executor, properties);
    }

    @Test
    @DisplayName("Should query IGDB and map the successful response")
    void searchGamesSuccessfully() {
        server.expect(requestTo(GAMES_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Client-ID", "test-client"))
                .andExpect(header("Authorization", "Bearer token"))
                .andExpect(content().string(containsString("search \"zelda\";")))
                .andExpect(content().string(containsString("limit 10;")))
                .andRespond(withSuccess("""
                        [{"id":1,"name":"Zelda","summary":"An adventure",
                          "cover":{"url":"//images.igdb.com/cover.jpg"},
                          "genres":[{"name":"Adventure"}],
                          "platforms":[{"name":"Switch"}]}]
                        """, MediaType.APPLICATION_JSON));

        List<IgdbGameResponse> games = client.searchGames("zelda");

        assertEquals(1, games.size());
        assertEquals(1L, games.get(0).id());
        assertEquals("Zelda", games.get(0).name());
        assertEquals("//images.igdb.com/cover.jpg", games.get(0).cover().url());
        server.verify();
    }

    @Test
    @DisplayName("Should return an empty list for a legitimate empty response")
    void returnsEmptyListForEmptyResponse() {
        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.searchGames("zelda").isEmpty());
        server.verify();
    }

    @Test
    @DisplayName("Should fail when IGDB returns a null body")
    void failsOnNullBody() {
        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andRespond(withSuccess());

        ExternalApiException exception = assertThrows(ExternalApiException.class,
                () -> client.searchGames("zelda"));

        assertFalse(exception instanceof ExternalApiAuthenticationException);
        assertFalse(exception instanceof ExternalApiRateLimitException);
        server.verify();
    }

    @Test
    @DisplayName("Should fail when IGDB returns an unparseable body")
    void failsOnUnexpectedBody() {
        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andRespond(withSuccess("{not-json", MediaType.APPLICATION_JSON));

        assertThrows(ExternalApiException.class, () -> client.searchGames("zelda"));
        server.verify();
    }

    @Test
    @DisplayName("Should renew the token once and retry when IGDB returns 401")
    void renewsTokenOnceOnUnauthorized() {
        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andRespond(withUnauthorizedRequest());
        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.searchGames("zelda").isEmpty());
        verify(authService, times(1)).invalidateToken();
        server.verify();
    }

    @Test
    @DisplayName("Should give up when IGDB keeps returning 401")
    void failsWhenUnauthorizedPersists() {
        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andRespond(withUnauthorizedRequest());
        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andRespond(withUnauthorizedRequest());

        assertThrows(ExternalApiAuthenticationException.class, () -> client.searchGames("zelda"));
        verify(authService, times(1)).invalidateToken();
        server.verify();
    }

    @Test
    @DisplayName("Should map 403 to an external authentication failure without renewing the token")
    void mapsForbiddenToAuthenticationFailure() {
        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andRespond(withForbiddenRequest());

        assertThrows(ExternalApiAuthenticationException.class, () -> client.searchGames("zelda"));
        verify(authService, never()).invalidateToken();
        server.verify();
    }

    @Test
    @DisplayName("Should map other 4xx responses to a generic external API failure")
    void mapsOtherClientErrors() {
        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andRespond(withBadRequest());

        ExternalApiException exception = assertThrows(ExternalApiException.class,
                () -> client.searchGames("zelda"));

        assertFalse(exception instanceof ExternalApiAuthenticationException);
        assertFalse(exception instanceof ExternalApiRateLimitException);
        server.verify();
    }

    @Test
    @DisplayName("Should retry 5xx responses and then fail")
    void retriesServerErrors() {
        server.expect(ExpectedCount.times(3), requestTo(GAMES_URL))
                .andRespond(withServerError());

        assertThrows(ExternalApiException.class, () -> client.searchGames("zelda"));
        verify(authService, never()).invalidateToken();
        server.verify();
    }

    @Test
    @DisplayName("Should map 429 to a rate limit failure carrying Retry-After")
    void mapsTooManyRequests() {
        server.expect(ExpectedCount.times(3), requestTo(GAMES_URL))
                .andRespond(withTooManyRequests(1));

        ExternalApiRateLimitException exception = assertThrows(ExternalApiRateLimitException.class,
                () -> client.searchGames("zelda"));

        assertEquals(1L, exception.getRetryAfterSeconds());
        server.verify();
    }

    @Test
    @DisplayName("Should stop retrying 429 when Retry-After exceeds the budget")
    void stopsRetryingWhenRetryAfterExceedsBudget() {
        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andRespond(withTooManyRequests(5));

        ExternalApiRateLimitException exception = assertThrows(ExternalApiRateLimitException.class,
                () -> client.searchGames("zelda"));

        assertEquals(5L, exception.getRetryAfterSeconds());
        server.verify();
    }

    @Test
    @DisplayName("Should use the configured result limit")
    void usesConfiguredLimit() {
        client = clientWithLimit(25);

        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andExpect(content().string(containsString("limit 25;")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.searchGames("zelda").isEmpty());
        server.verify();
    }

    @Test
    @DisplayName("Should reject null, blank or out of range search terms")
    void rejectsInvalidSearchTerms() {
        assertThrows(IllegalArgumentException.class, () -> client.searchGames(null));
        assertThrows(IllegalArgumentException.class, () -> client.searchGames(""));
        assertThrows(IllegalArgumentException.class, () -> client.searchGames("   "));
        assertThrows(IllegalArgumentException.class, () -> client.searchGames("a"));
        assertThrows(IllegalArgumentException.class, () -> client.searchGames("a".repeat(201)));
        server.verify();
    }

    @Test
    @DisplayName("Should trim search terms before building the query")
    void trimsSearchTerms() {
        server.expect(ExpectedCount.once(), requestTo(GAMES_URL))
                .andExpect(content().string(containsString("search \"zelda\";")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.searchGames("  zelda  ").isEmpty());
        server.verify();
    }

    @Test
    @DisplayName("Should escape quotes and backslashes in the search term")
    void escapesQuotesAndBackslashes() {
        assertEquals("he\\\"llo\\\\world", IgdbClient.escapeSearchTerm("he\"llo\\world"));
    }

    @Test
    @DisplayName("Should strip control characters from the search term")
    void stripsControlCharacters() {
        assertEquals("abc", IgdbClient.escapeSearchTerm("a\u0001b\u0002c"));
        assertEquals("line", IgdbClient.escapeSearchTerm("li\nne"));
    }
}
