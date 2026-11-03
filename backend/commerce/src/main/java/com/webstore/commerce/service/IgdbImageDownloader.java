package com.webstore.commerce.service;

import com.webstore.commerce.exception.errors.ExternalApiException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

@Component
public class IgdbImageDownloader {
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public DownloadedImage download(String imageId, String size) {
        if (imageId == null || !imageId.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new ExternalApiException("IGDB returned an invalid image identifier");
        }
        String url = "https://images.igdb.com/igdb/image/upload/" + size + "/" + imageId + ".jpg";
        try {
            HttpResponse<byte[]> response = client.send(HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            String contentType = response.headers().firstValue("Content-Type").orElse("")
                    .split(";")[0].toLowerCase(Locale.ROOT);
            if (response.statusCode() != 200 || response.body().length == 0
                    || response.body().length > MAX_IMAGE_BYTES
                    || !java.util.Set.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
                throw new ExternalApiException("IGDB image could not be downloaded or is not a supported image");
            }
            return new DownloadedImage(response.body(), contentType, imageId + ".jpg");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException("IGDB image download was interrupted", e);
        } catch (IOException | IllegalArgumentException e) {
            throw new ExternalApiException("Unable to download an IGDB image", e);
        }
    }

    public record DownloadedImage(byte[] bytes, String contentType, String filename) {}
}
