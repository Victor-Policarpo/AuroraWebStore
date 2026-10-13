package com.webstore.commerce.dto.request;

import com.webstore.commerce.entity.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(
        @NotNull(message = "This field is required and cannot be left blank.")
        ProductStatus status
) {
}