package com.webstore.commerce.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        String slug,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
