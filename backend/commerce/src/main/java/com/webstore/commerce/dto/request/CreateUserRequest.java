package com.webstore.commerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "This field is required and cannot be left blank.")
        @Size(min = 3, max = 100, message = "This field must contain 3 to 150 characters.")
        String name,

        @Email(message = "This field must be a valid email address.")
        String email,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).*$",
                message = "Your password must contain at least one special character, one number, and one letter."
        )
        @Size(min = 8, max = 16, message = "This field must contain 8 to 16 characters.")
        String password
) {
}
