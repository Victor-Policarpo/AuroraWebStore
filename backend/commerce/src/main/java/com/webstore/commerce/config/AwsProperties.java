package com.webstore.commerce.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "cloud.aws")
public class AwsProperties {

    private final Credentials credentials = new Credentials();
    private final S3 s3 = new S3();
    private final Region region = new Region();

    @Getter
    @Setter
    public static class Credentials {
        @NotBlank(message = "Aws Access Key must be provided")
        private String accessKey;

        @NotBlank(message = "Aws Secret Key must be provided")
        private String secretKey;
    }

    @Getter
    @Setter
    public static class S3 {
        @NotBlank(message = "Aws Bucket must be provided")
        private String bucket;
    }

    @Getter
    @Setter
    public static class Region {
        @NotBlank(message = "Aws Region must be provided")
        private String staticRegion;

    }

}
