package com.webstore.commerce.integration.igdb;

import com.webstore.commerce.integration.igdb.dto.IgdbGameResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class IgdbClient {

    private final RestClient igdbRestClient;
    private final IgdbAuthService igdbAuthService;
    private final String clientId;

    public IgdbClient(
            @Qualifier("igdbRestClient") RestClient igdbRestClient,
            IgdbAuthService igdbAuthService,
            @Value("${igdb.client-id}") String clientId
    ) {
        this.igdbRestClient = igdbRestClient;
        this.igdbAuthService = igdbAuthService;
        this.clientId = clientId;
    }

    public List<IgdbGameResponse> searchGames(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            throw new IllegalArgumentException(
                    "Search term must not be blank"
            );
        }

        String accessToken = igdbAuthService.getAccessToken();

        String query = """
                search "%s";
                fields id, name, summary, cover.url,
                       genres.name, platforms.name,
                       rating, first_release_date;
                limit 10;
                """.formatted(escapeSearchTerm(searchTerm));

        List<IgdbGameResponse> response = igdbRestClient.post()
                .uri("/games")
                .header("Client-ID", clientId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.TEXT_PLAIN)
                .body(query)
                .retrieve()
                .body(new ParameterizedTypeReference<List<IgdbGameResponse>>() {});

        return response != null ? response : List.of();
    }

    private String escapeSearchTerm(String searchTerm) {
        return searchTerm
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}