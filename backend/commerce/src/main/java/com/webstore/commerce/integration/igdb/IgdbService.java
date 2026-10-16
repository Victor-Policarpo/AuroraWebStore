package com.webstore.commerce.integration.igdb;

import com.webstore.commerce.dto.response.IgdbGameSearchResponse;
import com.webstore.commerce.integration.igdb.dto.IgdbGameResponse;
import com.webstore.commerce.integration.igdb.dto.IgdbNamedResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IgdbService {

    private final IgdbClient igdbClient;

    public IgdbService(IgdbClient igdbClient) {
        this.igdbClient = igdbClient;
    }

    public List<IgdbGameSearchResponse> searchGames(String searchTerm) {
        return igdbClient.searchGames(searchTerm)
                .stream()
                .map(this::toSearchResponse)
                .toList();
    }

    private IgdbGameSearchResponse toSearchResponse(IgdbGameResponse game) {
        String coverUrl = null;

        if (game.cover() != null && game.cover().url() != null) {
            coverUrl = normalizeImageUrl(game.cover().url());
        }

        List<String> genres = extractNames(game.genres());
        List<String> platforms = extractNames(game.platforms());

        return new IgdbGameSearchResponse(
                game.id(),
                game.name(),
                game.summary(),
                coverUrl,
                genres,
                platforms
        );
    }

    private List<String> extractNames(List<IgdbNamedResponse> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .filter(item -> item != null && item.name() != null)
                .map(IgdbNamedResponse::name)
                .toList();
    }

    private String normalizeImageUrl(String url) {
        if (url.startsWith("//")) {
            return "https:" + url;
        }

        return url;
    }
}