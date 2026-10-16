package com.webstore.commerce.exception;

import com.webstore.commerce.exception.errors.ExternalApiAuthenticationException;
import com.webstore.commerce.exception.errors.ExternalApiException;
import com.webstore.commerce.exception.errors.ExternalApiRateLimitException;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should map rate limit failures to 429 and expose Retry-After")
    void mapsRateLimitWithRetryAfter() throws Exception {
        mockMvc.perform(get("/test/rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "12"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("External API rate limit exceeded"))
                .andExpect(jsonPath("$.message").value("IGDB rate limit exceeded"))
                .andExpect(jsonPath("$.path").value("/test/rate-limit"));
    }

    @Test
    @DisplayName("Should not expose Retry-After when no valid value is available")
    void omitsInvalidRetryAfter() throws Exception {
        mockMvc.perform(get("/test/rate-limit-without-retry"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().doesNotExist("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("Should map generic integration failures to 502")
    void mapsExternalApiFailureToBadGateway() throws Exception {
        mockMvc.perform(get("/test/external"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("External API error"))
                .andExpect(jsonPath("$.message").value("IGDB failed"));
    }

    @Test
    @DisplayName("Should map external authentication failures to 502")
    void mapsExternalAuthenticationFailureToBadGateway() throws Exception {
        mockMvc.perform(get("/test/external-auth"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("External API authentication failed"));
    }

    @Test
    @DisplayName("Should keep mapping resource not found to 404")
    void keepsResourceNotFoundHandler() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"));
    }

    @RestController
    static class FailingController {

        @GetMapping("/test/rate-limit")
        void rateLimit() {
            throw new ExternalApiRateLimitException("IGDB rate limit exceeded", 12L);
        }

        @GetMapping("/test/rate-limit-without-retry")
        void rateLimitWithoutRetry() {
            throw new ExternalApiRateLimitException("IGDB rate limit exceeded");
        }

        @GetMapping("/test/external")
        void external() {
            throw new ExternalApiException("IGDB failed");
        }

        @GetMapping("/test/external-auth")
        void externalAuth() {
            throw new ExternalApiAuthenticationException("IGDB rejected the token");
        }

        @GetMapping("/test/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Missing");
        }
    }
}
