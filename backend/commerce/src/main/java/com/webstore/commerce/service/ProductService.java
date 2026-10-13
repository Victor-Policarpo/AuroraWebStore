package com.webstore.commerce.service;

import com.webstore.commerce.dto.request.CreateProductRequest;
import com.webstore.commerce.dto.request.UpdateProductRequest;
import com.webstore.commerce.dto.request.UpdateProductStatusRequest;
import com.webstore.commerce.dto.response.ProductResponse;
import com.webstore.commerce.entity.Category;
import com.webstore.commerce.entity.Product;
import com.webstore.commerce.entity.enums.ProductStatus;
import com.webstore.commerce.exception.errors.BadCredentialsException;
import com.webstore.commerce.exception.errors.ResourceAlreadyExistsException;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.mapper.ProductMapper;
import com.webstore.commerce.repository.CategoryRepository;
import com.webstore.commerce.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsBySlug(request.slug())) {
            throw new ResourceAlreadyExistsException("Product with this slug already exists");
        }
        if (request.igdbId() != null && productRepository.existsByIgdbId(request.igdbId())) {
            throw new ResourceAlreadyExistsException("Product with this IGDB ID already exists");
        }
        Product product = productMapper.toEntity(request);
        product.setCategory(getCategory(request.categoryId()));
        product.setStatus(ProductStatus.DRAFT);
        return productMapper.toResponse(productRepository.save(product));
    }

    public Page<ProductResponse> findAll(ProductStatus status, UUID categoryId, Pageable pageable) {
        return productRepository.search(status, categoryId, pageable).map(productMapper::toResponse);
    }

    public ProductResponse findById(UUID id) {
        return productMapper.toResponse(getProduct(id));
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = getProduct(id);
        if (request.slug() != null && !request.slug().equals(product.getSlug())
                && productRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new ResourceAlreadyExistsException("Product with this slug already exists");
        }
        if (request.igdbId() != null && !request.igdbId().equals(product.getIgdbId())
                && productRepository.existsByIgdbIdAndIdNot(request.igdbId(), id)) {
            throw new ResourceAlreadyExistsException("Product with this IGDB ID already exists");
        }
        productMapper.updateEntity(request, product);
        if (request.categoryId() != null) {
            product.setCategory(getCategory(request.categoryId()));
        }
        return productMapper.toResponse(productRepository.save(product));
    }


    @Transactional
    public ProductResponse updateStatus(UUID id, UpdateProductStatusRequest request) {
        Product product = getProduct(id);
        if (request.status() == null) {
            throw new IllegalArgumentException("Product status is required.");
        }
        product.setStatus(request.status());
        return productMapper.toResponse(productRepository.save(product));
    }


    private Category getCategory(UUID id) {
        return categoryRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Category not found")
        );
    }

    private Product getProduct(UUID id) {
        return productRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Product not found")
        );
    }
}