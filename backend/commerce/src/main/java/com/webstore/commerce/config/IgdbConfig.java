package com.webstore.commerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class IgdbConfig {

    @Value("${igdb.api.base-url}")
    private String baseUrl;

    @Bean("igdbRestClient")
    public RestClient igdbRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean("twitchAuthRestClient")
    public RestClient twitchAuthRestClient() {
        return RestClient.builder().build();
    }
}


