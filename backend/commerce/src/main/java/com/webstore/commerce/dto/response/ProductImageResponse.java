package com.webstore.commerce.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String originalFileName,
        String contentType,
        Long fileSize,
        Instant createdAt,
        String url
) {
}
