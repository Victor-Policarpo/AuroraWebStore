package com.webstore.commerce.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class IgdbPropertiesTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("Should accept a complete valid configuration")
    void acceptsValidConfiguration() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    @DisplayName("Should accept the limit boundaries")
    void acceptsLimitBoundaries() {
        IgdbProperties lower = valid();
        lower.getSearch().setLimit(1);
        assertTrue(validator.validate(lower).isEmpty());

        IgdbProperties upper = valid();
        upper.getSearch().setLimit(50);
        assertTrue(validator.validate(upper).isEmpty());
    }

    @Test
    @DisplayName("Should default to a limit of 10")
    void defaultsLimitToTen() {
        IgdbProperties properties = new IgdbProperties();
        assertEquals(10, properties.getSearch().getLimit());
    }

    @Test
    @DisplayName("Should reject a missing or blank base url")
    void rejectsMissingBaseUrl() {
        IgdbProperties missing = valid();
        missing.getApi().setBaseUrl(null);
        assertFalse(validator.validate(missing).isEmpty());

        IgdbProperties blank = valid();
        blank.getApi().setBaseUrl("   ");
        assertFalse(validator.validate(blank).isEmpty());
    }

    @Test
    @DisplayName("Should reject a missing or blank auth url")
    void rejectsMissingAuthUrl() {
        IgdbProperties missing = valid();
        missing.getAuth().setUrl(null);
        assertFalse(validator.validate(missing).isEmpty());

        IgdbProperties blank = valid();
        blank.getAuth().setUrl("");
        assertFalse(validator.validate(blank).isEmpty());
    }

    @Test
    @DisplayName("Should reject urls without an http or https scheme")
    void rejectsNonHttpUrls() {
        IgdbProperties ftp = valid();
        ftp.getApi().setBaseUrl("ftp://api.igdb.com/v4");
        assertFalse(validator.validate(ftp).isEmpty());

        IgdbProperties schemeLess = valid();
        schemeLess.getAuth().setUrl("id.twitch.tv/oauth2/token");
        assertFalse(validator.validate(schemeLess).isEmpty());
    }

    @Test
    @DisplayName("Should reject malformed urls and urls without a host")
    void rejectsMalformedUrls() {
        IgdbProperties malformed = valid();
        malformed.getApi().setBaseUrl("https://exa mple.com");
        assertFalse(validator.validate(malformed).isEmpty());

        IgdbProperties noHost = valid();
        noHost.getAuth().setUrl("https://");
        assertFalse(validator.validate(noHost).isEmpty());
    }

    @Test
    @DisplayName("Should reject an out of range limit")
    void rejectsInvalidLimit() {
        IgdbProperties tooLow = valid();
        tooLow.getSearch().setLimit(0);
        assertFalse(validator.validate(tooLow).isEmpty());

        IgdbProperties tooHigh = valid();
        tooHigh.getSearch().setLimit(51);
        assertFalse(validator.validate(tooHigh).isEmpty());
    }

    private IgdbProperties valid() {
        IgdbProperties properties = new IgdbProperties();
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.getApi().setBaseUrl("https://api.igdb.com/v4");
        properties.getAuth().setUrl("https://id.twitch.tv/oauth2/token");
        return properties;
    }

    @Test
    @DisplayName("Should bind and validate the configuration at startup")
    void SpringRejectsInvalidConfigurationAtStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues(
                        "igdb.api.base-url=https://api.igdb.com/v4",
                        "igdb.auth.url=https://id.twitch.tv/oauth2/token",
                        "igdb.search.limit=25")
                .run(context -> assertThat(context).hasNotFailed());

        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues("igdb.auth.url=https://id.twitch.tv/oauth2/token")
                .run(context -> assertThat(context).hasFailed());

        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues(
                        "igdb.api.base-url=ftp://api.igdb.com/v4",
                        "igdb.auth.url=https://id.twitch.tv/oauth2/token")
                .run(context -> assertThat(context).hasFailed());

        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues(
                        "igdb.api.base-url=https://api.igdb.com/v4",
                        "igdb.auth.url=https://id.twitch.tv/oauth2/token",
                        "igdb.search.limit=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(IgdbProperties.class)
    static class PropertiesConfiguration {
    }
}
