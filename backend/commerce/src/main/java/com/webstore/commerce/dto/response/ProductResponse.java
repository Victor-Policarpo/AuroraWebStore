package com.webstore.commerce.dto.response;

import com.webstore.commerce.entity.enums.ProductStatus;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID categoryId,
        String name,
        String description,
        String slug,
        String brand,
        String platform,
        String region,
        Long basePrice,
        String baseCurrency,
        ProductStatus status,
        Long igdbId,
        Long steamAppId,
        Instant createdAt,
        Instant updatedAt
) {
}