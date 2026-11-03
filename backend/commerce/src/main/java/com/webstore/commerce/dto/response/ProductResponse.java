package com.webstore.commerce.dto.response;

import com.webstore.commerce.entity.enums.ProductStatus;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record ProductResponse(
        UUID id,
        List<UUID> categoryIds,
        UUID productTypeId,
        String name,
        String description,
        String trailerUrl,
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
) {}
