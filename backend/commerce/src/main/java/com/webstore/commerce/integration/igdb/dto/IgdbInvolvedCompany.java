package com.webstore.commerce.integration.igdb.dto;

public record IgdbInvolvedCompany(
        IgdbNamedResponse company,
        Boolean developer,
        Boolean publisher
) {}
