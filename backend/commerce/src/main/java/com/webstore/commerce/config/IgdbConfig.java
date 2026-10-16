package com.webstore.commerce.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IgdbProperties.class)
public class IgdbConfig {

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    private final IgdbProperties properties;

    public IgdbConfig(IgdbProperties properties) {
        this.properties = properties;
    }

    @Bean("igdbRestClient")
    public RestClient igdbRestClient() {
        return RestClient.builder()
                .baseUrl(properties.getApi().getBaseUrl())
                .requestFactory(requestFactory())
                .build();
    }

    @Bean("twitchAuthRestClient")
    public RestClient twitchAuthRestClient() {
        return RestClient.builder()
                .requestFactory(requestFactory())
                .build();
    }

    private ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS));
        factory.setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MS));

        return factory;
    }
}