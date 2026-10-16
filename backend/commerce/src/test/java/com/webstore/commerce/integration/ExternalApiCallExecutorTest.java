package com.webstore.commerce.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalApiCallExecutorTest {

    private ExternalApiCallExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ExternalApiCallExecutor(millis -> { }, new Random(0));
    }

    @Test
    @DisplayName("Should return the result without retrying on success")
    void returnsResultOnSuccess() {
        assertEquals("ok", executor.execute("op", () -> "ok"));
    }

    @Test
    @DisplayName("Should retry server errors up to the maximum number of attempts")
    void retriesServerErrorsUpToMaxAttempts() {
        AtomicInteger attempts = new AtomicInteger();

        HttpServerErrorException exception = assertThrows(HttpServerErrorException.class, () ->
                executor.execute("op", () -> {
                    attempts.incrementAndGet();
                    throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
                }));

        assertEquals(ExternalApiCallExecutor.MAX_ATTEMPTS, attempts.get());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

    @Test
    @DisplayName("Should retry network failures up to the maximum number of attempts")
    void retriesNetworkFailures() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(ResourceAccessException.class, () ->
                executor.execute("op", () -> {
                    attempts.incrementAndGet();
                    throw new ResourceAccessException("timeout");
                }));

        assertEquals(ExternalApiCallExecutor.MAX_ATTEMPTS, attempts.get());
    }

    @Test
    @DisplayName("Should not retry non transient client errors")
    void doesNotRetryClientErrors() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(HttpClientErrorException.BadRequest.class, () ->
                executor.execute("op", () -> {
                    attempts.incrementAndGet();
                    throw HttpClientErrorException.create(
                            HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                            new byte[0], StandardCharsets.UTF_8);
                }));

        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("Should respect Retry-After for 429 when it fits the wait budget")
    void respectsRetryAfterWithinBudget() {
        AtomicInteger attempts = new AtomicInteger();

        HttpClientErrorException.TooManyRequests exception = assertThrows(
                HttpClientErrorException.TooManyRequests.class, () ->
                        executor.execute("op", () -> {
                            attempts.incrementAndGet();
                            throw tooManyRequests(1);
                        }));

        assertEquals(ExternalApiCallExecutor.MAX_ATTEMPTS, attempts.get());
        assertEquals("1", exception.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER));
    }

    @Test
    @DisplayName("Should stop retrying 429 when Retry-After exceeds the wait budget")
    void stopsWhenRetryAfterExceedsBudget() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(HttpClientErrorException.TooManyRequests.class, () ->
                executor.execute("op", () -> {
                    attempts.incrementAndGet();
                    throw tooManyRequests(5);
                }));

        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("Should recover from a single 401 using the authentication recovery hook")
    void recoversFromUnauthorizedOnce() {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger recoveries = new AtomicInteger();

        String result = executor.execute("op", () -> {
            if (attempts.incrementAndGet() == 1) {
                throw unauthorized();
            }
            return "ok";
        }, () -> {
            recoveries.incrementAndGet();
            return true;
        });

        assertEquals("ok", result);
        assertEquals(2, attempts.get());
        assertEquals(1, recoveries.get());
    }

    @Test
    @DisplayName("Should not recover more than once from repeated 401 responses")
    void doesNotRecoverRepeatedly() {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger recoveries = new AtomicInteger();

        assertThrows(HttpClientErrorException.Unauthorized.class, () ->
                executor.execute("op", () -> {
                    attempts.incrementAndGet();
                    throw unauthorized();
                }, () -> {
                    recoveries.incrementAndGet();
                    return true;
                }));

        assertEquals(2, attempts.get());
        assertEquals(1, recoveries.get());
    }

    private static HttpClientErrorException.TooManyRequests tooManyRequests(int retryAfterSeconds) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, Integer.toString(retryAfterSeconds));
        return (HttpClientErrorException.TooManyRequests) HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers,
                new byte[0], StandardCharsets.UTF_8);
    }

    private static HttpClientErrorException.Unauthorized unauthorized() {
        return (HttpClientErrorException.Unauthorized) HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8);
    }
}
