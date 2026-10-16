package com.webstore.commerce.integration.igdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IgdbTokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("token_type")
        String tokenType
) {
}