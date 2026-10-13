package com.webstore.commerce.integration.igdb.dto;

import java.util.List;

public record IgdbGameResponse(
        Long id,
        String name,
        String summary,
        IgdbCoverResponse cover,
        List<IgdbNamedResponse> genres,
        List<IgdbNamedResponse> platforms,
        Double rating,
        Long firstReleaseDate
) {}