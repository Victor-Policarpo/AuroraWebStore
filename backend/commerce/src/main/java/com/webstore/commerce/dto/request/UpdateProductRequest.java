package com.webstore.commerce.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateProductRequest(
        @Size(max = 200, message = "This field must contain at most 200 characters.")
        String name,

        @Size(max = 500, message = "This field must contain at most 500 characters.")
        String description,

        @Size(min = 1, max = 255, message = "This field must contain 1 to 255 characters.")
        String slug,

        @Size(max = 100, message = "This field must contain at most 100 characters.")
        String brand,

        @Size(max = 100, message = "This field must contain at most 100 characters.")
        String platform,

        @Size(max = 100, message = "This field must contain at most 100 characters.")
        String region,

        @PositiveOrZero(message = "This field must be zero or a positive number.")
        Long basePrice,

        @Size(max = 3, message = "This field must contain at most 3 characters.")
        @Pattern(regexp = "^(BRL|USD|EUR)$", message = "Currency must be one of: BRL, USD, EUR.")
        String baseCurrency,

        @Positive(message = "This field must be a positive number.")
        Long igdbId,

        @Positive(message = "This field must be a positive number.")
        Long steamAppId,

        UUID categoryId
) {
}