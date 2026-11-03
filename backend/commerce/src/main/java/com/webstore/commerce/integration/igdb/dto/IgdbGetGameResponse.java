package com.webstore.commerce.integration.igdb.dto;

import java.util.List;

public record IgdbGetGameResponse(
        Long igdbId,
        String name,
        String summary,
        String trailerUrl,
        String coverImageId,
        List<String> artworksImageIds,
        List<String> screenshotsImageIds,
        List<String> genres,
        List<String> platforms,
        List<IgdbCompanyResponse> companies
) {}
