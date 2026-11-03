package com.webstore.commerce.integration.igdb.dto;

public record IgdbCompanyResponse(
        String name,
        Boolean developer,
        Boolean publisher
) {}
