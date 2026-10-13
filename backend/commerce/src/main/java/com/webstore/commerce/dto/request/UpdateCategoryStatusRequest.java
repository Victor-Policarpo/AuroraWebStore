package com.webstore.commerce.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateCategoryStatusRequest(
        @NotNull(message = "This field is required and cannot be left blank.")
        Boolean active
) {
}
