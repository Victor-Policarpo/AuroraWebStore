package com.webstore.commerce.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProductTypeResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
