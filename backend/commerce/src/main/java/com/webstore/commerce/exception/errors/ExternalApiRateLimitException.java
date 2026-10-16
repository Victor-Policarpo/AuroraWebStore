package com.webstore.commerce.exception.errors;

public class ExternalApiRateLimitException extends ExternalApiException {

    private final Long retryAfterSeconds;

    public ExternalApiRateLimitException(String message) {
        this(message, null);
    }

    public ExternalApiRateLimitException(String message, Long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
