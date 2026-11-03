package com.webstore.commerce.service;

import com.webstore.commerce.dto.request.CreateProductTypeRequest;
import com.webstore.commerce.dto.request.UpdateProductTypeRequest;
import com.webstore.commerce.dto.request.UpdateProductTypeStatusRequest;
import com.webstore.commerce.dto.response.ProductTypeResponse;
import com.webstore.commerce.entity.ProductType;
import com.webstore.commerce.exception.errors.ResourceAlreadyExistsException;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.mapper.ProductTypeMapper;
import com.webstore.commerce.repository.ProductTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductTypeService {

    private final ProductTypeRepository productTypeRepository;
    private final ProductTypeMapper productTypeMapper;

    @Transactional
    public ProductTypeResponse create(CreateProductTypeRequest request) {
        if (productTypeRepository.existsByName(request.name())) {
            throw new ResourceAlreadyExistsException("Product type with this name already exists");
        }
        ProductType productType = productTypeMapper.toEntity(request);
        productType.setActive(true);
        return productTypeMapper.toResponse(productTypeRepository.save(productType));
    }

    public Page<ProductTypeResponse> findAll(Pageable pageable) {
        return productTypeRepository.findAll(pageable).map(productTypeMapper::toResponse);
    }

    public ProductTypeResponse findById(UUID id) {
        return productTypeMapper.toResponse(getProductType(id));
    }

    @Transactional
    public ProductTypeResponse update(UUID id, UpdateProductTypeRequest request) {
        ProductType productType = getProductType(id);
        if (request.name() != null && !request.name().equals(productType.getName())
                && productTypeRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ResourceAlreadyExistsException("Product type with this name already exists");
        }
        productTypeMapper.updateEntity(request, productType);
        return productTypeMapper.toResponse(productTypeRepository.save(productType));
    }

    @Transactional
    public ProductTypeResponse updateStatus(UUID id, UpdateProductTypeStatusRequest request) {
        ProductType productType = getProductType(id);
        productType.setActive(request.active());
        return productTypeMapper.toResponse(productTypeRepository.save(productType));
    }

    private ProductType getProductType(UUID id) {
        return productTypeRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Product type not found")
        );
    }
}
