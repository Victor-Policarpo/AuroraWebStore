package com.webstore.commerce.service;

import com.webstore.commerce.config.AwsProperties;
import com.webstore.commerce.dto.response.ProductImageResponse;
import com.webstore.commerce.entity.Product;
import com.webstore.commerce.entity.ProductImage;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.mapper.ProductImageMapper;
import com.webstore.commerce.repository.ProductImageRepository;
import com.webstore.commerce.repository.ProductRepository;
import com.webstore.commerce.exception.errors.BadRequestException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final AwsProperties AwsProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ProductImageMapper productImageMapper;

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpeg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif",
            "video/mp4", ".mp4",
            "video/webm", ".webm"
    );
    private final AwsProperties awsProperties;

    @Value("${spring.servlet.multipart.max-file-size}")
    private DataSize maxFileSize;

    @Value("${app.max-image-per-product}")
    private int maxImagePerProduct;

    @Transactional
    public void uploadImages(List<MultipartFile> file, UUID productId) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found or not exists"));
        long currentImages = productImageRepository.countByProductId(productId);
        if (currentImages + file.size() > maxImagePerProduct) {
            throw new BadRequestException("You can only upload " + maxImagePerProduct +
                    " images per Product and you already have " + currentImages + " images uploaded."
            );
        }

        Integer maxDisplayOrder = productImageRepository.findMaxDisplayOrderByProductId(productId);
        int displayOrder = maxDisplayOrder == null ? 0 : maxDisplayOrder + 1;
        List<String> uploadedKeys = new ArrayList<>();

        try {
            for (MultipartFile multipartFile : file) {
                validateFile(multipartFile);
                String key = createImagePath(multipartFile.getContentType(), productId);

                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(awsProperties.getS3().getBucket())
                                .key(key)
                                .contentType(multipartFile.getContentType())
                                .build(),
                        RequestBody.fromBytes(multipartFile.getBytes())
                );
                uploadedKeys.add(key);
                productImageRepository.save(
                        productImageMapper.toEntity(multipartFile, key, product, displayOrder++)
                );
            }
        } catch (Exception e) {
            for (String key : uploadedKeys) {
                try {
                    s3Client.deleteObject(
                            DeleteObjectRequest.builder()
                                    .bucket(awsProperties.getS3().getBucket())
                                    .key(key)
                                    .build()
                    );
                } catch (Exception cleanupException) {
                    e.addSuppressed(cleanupException);
                }
            }
            throw e;
        }

    }

    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found or not exists"));
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found or not exists"));

        deleteImagesFromStorage(List.of(productImage));
        productImageRepository.delete(productImage);
    }

    @Transactional
    public void deleteAllImages(UUID productId) {
        List<ProductImage> images = productImageRepository.findByProductId(productId);
        if (images.isEmpty()) {
            throw new ResourceNotFoundException("No images found for this product");
        }
        deleteImagesFromStorage(images);
        productImageRepository.deleteAll(images);
    }


    public List<ProductImageResponse> getProductImages(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found or not exists");
        }
        return toResponseDtoListWithPresignedUrls(productImageRepository.findByProductId(id));
    }

    public List<ProductImageResponse> toResponseDtoListWithPresignedUrls(List<ProductImage> images) {
        return images.stream()
                .map(image -> {
                    String presignedUrl = generatePresignedUrl(image.getS3Key());
                    return productImageMapper.toResponseDto(image, presignedUrl);
                })
                .toList();
    }

    public void deleteImagesFromStorage(List<ProductImage> images) {
        images.forEach(image ->
                s3Client.deleteObject(
                        DeleteObjectRequest.builder()
                                .bucket(awsProperties.getS3().getBucket())
                                .key(image.getS3Key())
                                .build()
                )
        );
    }

    private String generatePresignedUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsProperties.getS3().getBucket())
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(30))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String createImagePath(String contentType, UUID ProductId) {
        if (ProductId == null) {
            throw new BadRequestException("Product ID cannot be null");
        }

        String extension = CONTENT_TYPE_EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new BadRequestException("File type is not supported");
        }
        return "product-images/" + ProductId + "/" + UUID.randomUUID() + extension;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (!CONTENT_TYPE_EXTENSIONS.containsKey(file.getContentType())) {
            throw new BadRequestException("File type is not supported");
        }
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new BadRequestException("File size exceeds the maximum limit of " + maxFileSize);
        }
    }
}
