package com.webstore.commerce.integration.suppliers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SupplierOfferResponse(
        @NotBlank @Size(max = 100)
        String externalProductId,
        @NotNull @PositiveOrZero
        Long wholesalePrice,
        @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "BRL|USD|EUR")
        String wholesaleCurrency,
        @NotNull @PositiveOrZero
        Integer stockQuantity
) {
}
