package com.webstore.commerce.dto.response;

import java.util.List;

public record IgdbGameSearchResponse(
        Long igdbId,
        String name,
        String summary,
        String coverUrl,
        List<String> genres,
        List<String> platforms
) {
}
