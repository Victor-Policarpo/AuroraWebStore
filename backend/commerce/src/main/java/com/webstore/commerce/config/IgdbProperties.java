package com.webstore.commerce.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "igdb")
public class IgdbProperties {

    private String clientId;
    private String clientSecret;

    @Valid
    private Api api = new Api();

    @Valid
    private Auth auth = new Auth();

    @Valid
    private Search search = new Search();

    @Getter
    @Setter
    public static class Api {

        @NotBlank(message = "igdb.api.base-url must be provided")
        private String baseUrl;
    }

    @Getter
    @Setter
    public static class Auth {

        @NotBlank(message = "igdb.auth.url must be provided")
        private String url;
    }

    @Getter
    @Setter
    public static class Search {

        @Min(value = 1, message = "igdb.search.limit must be between 1 and 50")
        @Max(value = 50, message = "igdb.search.limit must be between 1 and 50")
        private int limit = 10;
    }

    @AssertTrue(message = "igdb.api.base-url must be a valid http or https URL")
    public boolean isApiBaseUrlValid() {
        return isValidHttpUrl(api == null ? null : api.getBaseUrl());
    }

    @AssertTrue(message = "igdb.auth.url must be a valid http or https URL")
    public boolean isAuthUrlValid() {
        return isValidHttpUrl(auth == null ? null : auth.getUrl());
    }

    private static boolean isValidHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();

            boolean httpScheme = "http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme);

            return httpScheme
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}