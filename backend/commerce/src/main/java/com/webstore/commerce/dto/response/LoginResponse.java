package com.webstore.commerce.dto.response;

public record LoginResponse(
        String token,
        long expiresIn
) {
}
