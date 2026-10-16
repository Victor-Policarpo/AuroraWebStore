package com.webstore.commerce.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

/**
 * Conservative retry helper for calls to external APIs.
 *
 * <p>It performs at most {@value #MAX_ATTEMPTS} attempts in a single flat loop
 * (never nested), retrying only transient failures: network problems, timeouts,
 * {@code 5xx} responses and {@code 429} rate limits. Permanent failures such as
 * authentication, authorization, validation or other {@code 4xx} responses are
 * propagated immediately.</p>
 */
@Slf4j
@Component
public class ExternalApiCallExecutor {

    static final int MAX_ATTEMPTS = 3;
    static final long MAX_TOTAL_WAIT_MILLIS = 2_000L;
    static final long INITIAL_BACKOFF_MILLIS = 150L;
    static final long MAX_BACKOFF_MILLIS = 600L;

    private final LongConsumer sleeper;
    private final Random random;

    public ExternalApiCallExecutor() {
        this(ExternalApiCallExecutor::sleep, new Random());
    }

    /**
     * Seam used by tests to avoid real waiting and to make jitter deterministic.
     */
    public ExternalApiCallExecutor(LongConsumer sleeper, Random random) {
        this.sleeper = sleeper;
        this.random = random;
    }

    public <T> T execute(String operation, Supplier<T> call) {
        return execute(operation, call, null);
    }

    /**
     * @param operation description used in diagnostics (must never contain secrets)
     * @param call the external call to perform
     * @param authenticationRecovery optional action invoked once on a {@code 401}
     *                               response; when it returns {@code true} the call
     *                               is retried with (presumably) refreshed credentials
     */
    public <T> T execute(String operation, Supplier<T> call, BooleanSupplier authenticationRecovery) {
        long totalWaited = 0L;
        boolean recovered = false;
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return call.get();
            } catch (HttpClientErrorException.Unauthorized e) {
                if (authenticationRecovery != null
                        && !recovered
                        && attempt < MAX_ATTEMPTS
                        && authenticationRecovery.getAsBoolean()) {
                    recovered = true;
                    continue;
                }
                throw e;
            } catch (RuntimeException e) {
                if (!isTransient(e)) {
                    throw e;
                }
                lastFailure = e;
                if (attempt >= MAX_ATTEMPTS) {
                    break;
                }
                long delay = delayMillis(e, attempt, totalWaited);
                if (delay < 0) {
                    break;
                }
                totalWaited += delay;
                sleeper.accept(delay);
            }
        }

        log.warn("External API call '{}' failed after {} attempt(s) ({})",
                operation, MAX_ATTEMPTS,
                lastFailure.getClass().getSimpleName());
        throw lastFailure;
    }

    private static boolean isTransient(RuntimeException exception) {
        return exception instanceof ResourceAccessException
                || exception instanceof HttpServerErrorException
                || exception instanceof HttpClientErrorException.TooManyRequests;
    }

    private long delayMillis(RuntimeException exception, int attempt, long totalWaited) {
        long requested;

        if (exception instanceof HttpClientErrorException.TooManyRequests tooManyRequests) {
            Long retryAfter = ExternalApiErrors.retryAfterSeconds(
                    tooManyRequests.getResponseHeaders());

            if (retryAfter == null) {
                requested = backoffMillis(attempt);
            } else {
                long remainingWait = MAX_TOTAL_WAIT_MILLIS - totalWaited;

                if (retryAfter > remainingWait / 1000L) {
                    return -1L;
                }

                requested = retryAfter * 1000L;
            }
        } else {
            requested = backoffMillis(attempt);
        }

        if (requested > MAX_TOTAL_WAIT_MILLIS - totalWaited) {
            return -1L;
        }

        return Math.max(requested, 0L);
    }

    private long backoffMillis(int attempt) {
        long exponential = Math.min(MAX_BACKOFF_MILLIS, INITIAL_BACKOFF_MILLIS << (attempt - 1));
        long half = exponential / 2;
        return half + random.nextLong(half + 1);
    }

    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
