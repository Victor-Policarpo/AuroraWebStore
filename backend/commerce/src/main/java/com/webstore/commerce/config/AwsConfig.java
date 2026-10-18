package com.webstore.commerce.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.List;

@Configuration()
@EnableConfigurationProperties(AwsProperties.class)
@RequiredArgsConstructor
public class AwsConfig {
    private final AwsProperties properties;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getCredentials().getAccessKey(),
                properties.getCredentials().getSecretKey()
        );
        return StaticCredentialsProvider.create(credentials);
    }

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getCredentials().getAccessKey(),
                properties.getCredentials().getSecretKey()
        );
        return S3Client.builder()
                .region(Region.of(properties.getRegion().getStaticRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(){
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.getCredentials().getAccessKey(),
                properties.getCredentials().getSecretKey()
        );

        return S3Presigner.builder()
                .region(Region.of(properties.getRegion().getStaticRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

}
