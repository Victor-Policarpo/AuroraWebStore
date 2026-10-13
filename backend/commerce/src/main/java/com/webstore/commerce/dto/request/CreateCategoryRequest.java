package com.webstore.commerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "This field is required and cannot be left blank.")
        @Size(max = 100, message = "This field must contain at most 100 characters.")
        String name,

        String description,

        @NotBlank(message = "This field is required and cannot be left blank.")
        @Size(max = 255, message = "This field must contain at most 255 characters.")
        String slug
) {
}
