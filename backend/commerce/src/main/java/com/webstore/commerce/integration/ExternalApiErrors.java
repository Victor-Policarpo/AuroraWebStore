package com.webstore.commerce.integration;

import com.webstore.commerce.exception.errors.ExternalApiAuthenticationException;
import com.webstore.commerce.exception.errors.ExternalApiException;
import com.webstore.commerce.exception.errors.ExternalApiRateLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientResponseException;

public final class ExternalApiErrors {

    private ExternalApiErrors() {
    }

    public static ExternalApiException map(String service, RestClientResponseException exception) {
        HttpStatusCode statusCode = exception.getStatusCode();
        int status = statusCode.value();

        if (status == 401 || status == 403) {
            return new ExternalApiAuthenticationException(
                    service + " rejected the provided credentials", exception);
        }

        if (status == 429) {
            return new ExternalApiRateLimitException(
                    service + " rate limit exceeded", retryAfterSeconds(exception.getResponseHeaders()));
        }

        return new ExternalApiException(
                service + " request failed with HTTP status " + status, exception);
    }

    public static Long retryAfterSeconds(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }

        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            long seconds = Long.parseLong(value.trim());
            return seconds >= 0 ? seconds : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
