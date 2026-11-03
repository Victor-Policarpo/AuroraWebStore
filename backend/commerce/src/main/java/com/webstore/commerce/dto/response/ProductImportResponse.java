package com.webstore.commerce.dto.response;

import com.webstore.commerce.entity.enums.ProductStatus;

import java.util.List;
import java.util.UUID;

public record ProductImportResponse(UUID productId, Long igdbId, String name, ProductStatus status,
                                   List<String> categoriesAssociated, List<String> unmatchedGenres) {}
