package com.webstore.commerce.controller;

import com.webstore.commerce.dto.response.ProductImageResponse;
import com.webstore.commerce.entity.ProductImage;
import com.webstore.commerce.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class ProductImageController {

    private final ProductImageService productImageService;

    @PostMapping("/{id}/images")
    public ResponseEntity<Void> uploadImages(@RequestParam("file") List<MultipartFile> file, @PathVariable UUID id) throws IOException {
        productImageService.uploadImages(file, id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @GetMapping("/{id}/images")
    public ResponseEntity<List<ProductImageResponse>> getImages(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(productImageService.getProductImages(id));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID productId, @PathVariable UUID imageId) {
        productImageService.deleteImage(productId, imageId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{productId}/images")
    public ResponseEntity<Void> deleteAllImages(@PathVariable UUID productId) {
        productImageService.deleteAllImages(productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
