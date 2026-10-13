package com.webstore.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProductRequest(
        @NotNull(message = "This field is required and cannot be left blank.")
        UUID categoryId,

        @NotBlank(message = "This field is required and cannot be left blank.")
        @Size(max = 200, message = "This field must contain at most 200 characters.")
        String name,

        String description,

        @NotBlank(message = "This field is required and cannot be left blank.")
        @Size(max = 255, message = "This field must contain at most 255 characters.")
        String slug,

        @Size(max = 100, message = "This field must contain at most 100 characters.")
        String brand,

        @NotBlank(message = "This field is required and cannot be left blank.")
        @Size(max = 100, message = "This field must contain at most 100 characters.")
        String platform,

        @NotBlank(message = "This field is required and cannot be left blank.")
        @Size(max = 100, message = "This field must contain at most 100 characters.")
        String region,

        @NotNull(message = "This field is required and cannot be left blank.")
        @PositiveOrZero(message = "This field must be zero or a positive number.")
        Long basePrice,

        @NotBlank(message = "This field is required and cannot be left blank.")
        @Size(max = 3, message = "This field must contain at most 3 characters.")
        @Pattern(regexp = "^(BRL|USD|EUR)$", message = "Currency must be one of: BRL, USD, EUR.")
        String baseCurrency,

        @Positive(message = "This field must be a positive number.")
        Long igdbId,

        @Positive(message = "This field must be a positive number.")
        Long steamAppId
) {
}