package com.webstore.commerce.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(min = 1, max = 100, message = "This field must contain 1 to 100 characters.")
        String name,

        @Size(max = 500, message = "This field must contain at most 500 characters.")
        String description,

        @Size(min = 1, max = 255, message = "This field must contain 1 to 255 characters.")
        String slug
) {
}
