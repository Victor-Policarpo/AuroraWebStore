package com.webstore.commerce.integration.igdb;

import com.webstore.commerce.config.IgdbProperties;
import com.webstore.commerce.exception.errors.ExternalApiException;
import com.webstore.commerce.integration.ExternalApiCallExecutor;
import com.webstore.commerce.integration.ExternalApiErrors;
import com.webstore.commerce.integration.igdb.dto.IgdbGameResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class IgdbClient {

    public static final int MIN_SEARCH_LENGTH = 2;
    public static final int MAX_SEARCH_LENGTH = 200;

    private final RestClient igdbRestClient;
    private final IgdbAuthService igdbAuthService;
    private final ExternalApiCallExecutor callExecutor;
    private final String clientId;
    private final int searchLimit;

    public IgdbClient(
            @Qualifier("igdbRestClient") RestClient igdbRestClient,
            IgdbAuthService igdbAuthService,
            ExternalApiCallExecutor callExecutor,
            IgdbProperties properties
    ) {
        this.igdbRestClient = igdbRestClient;
        this.igdbAuthService = igdbAuthService;
        this.callExecutor = callExecutor;
        this.clientId = properties.getClientId();
        this.searchLimit = properties.getSearch().getLimit();
    }

    public List<IgdbGameResponse> searchGames(String searchTerm) {
        String query = buildQuery(normalizeSearchTerm(searchTerm));

        try {
            return callExecutor.execute(
                    "IGDB game search",
                    () -> performSearch(query),
                    this::recoverAuthentication
            );
        } catch (RestClientResponseException e) {
            throw ExternalApiErrors.map("IGDB", e);
        } catch (ResourceAccessException e) {
            throw new ExternalApiException("Unable to reach the IGDB API", e);
        } catch (RestClientException e) {
            throw new ExternalApiException("Unexpected error while calling the IGDB API", e);
        }
    }

    private boolean recoverAuthentication() {
        igdbAuthService.invalidateToken();
        return true;
    }

    private List<IgdbGameResponse> performSearch(String query) {
        List<IgdbGameResponse> games = igdbRestClient.post()
                .uri("/games")
                .header("Client-ID", clientId)
                .header("Authorization", "Bearer " + igdbAuthService.getAccessToken())
                .contentType(MediaType.TEXT_PLAIN)
                .body(query)
                .retrieve()
                .body(new ParameterizedTypeReference<List<IgdbGameResponse>>() {});

        if (games == null) {
            throw new ExternalApiException("IGDB returned an empty or unexpected response body");
        }

        return games;
    }

    private String buildQuery(String searchTerm) {
        return """
                search "%s";
                fields id, name, summary, cover.url,
                       genres.name, platforms.name,
                       rating, first_release_date;
                limit %d;
                """.formatted(escapeSearchTerm(searchTerm), searchLimit);
    }

    static String normalizeSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            throw new IllegalArgumentException("Search term must not be blank");
        }

        String trimmed = searchTerm.trim();
        if (trimmed.length() < MIN_SEARCH_LENGTH || trimmed.length() > MAX_SEARCH_LENGTH) {
            throw new IllegalArgumentException(
                    "Search term must contain between " + MIN_SEARCH_LENGTH
                            + " and " + MAX_SEARCH_LENGTH + " characters");
        }

        return trimmed;
    }

    static String escapeSearchTerm(String searchTerm) {
        StringBuilder escaped = new StringBuilder(searchTerm.length());

        for (int i = 0; i < searchTerm.length(); i++) {
            char character = searchTerm.charAt(i);

            if (Character.isISOControl(character)) {
                continue;
            }

            if (character == '\\' || character == '"') {
                escaped.append('\\');
            }

            escaped.append(character);
        }

        return escaped.toString();
    }
}
