package com.webstore.commerce.integration.igdb.dto;

import java.util.List;

public record IgdbGame(
        Long id,
        String name,
        String summary,
        IgdbImage cover,
        List<IgdbImage> artworks,
        List<IgdbImage> screenshots,
        List<IgdbVideo> videos,
        List<IgdbNamedResponse> genres,
        List<IgdbNamedResponse> platforms,
        List<IgdbInvolvedCompany> involved_companies

) {}
