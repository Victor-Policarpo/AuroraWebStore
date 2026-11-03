package com.webstore.commerce.controller;

import com.webstore.commerce.dto.request.CreateProductTypeRequest;
import com.webstore.commerce.dto.request.UpdateProductTypeRequest;
import com.webstore.commerce.dto.request.UpdateProductTypeStatusRequest;
import com.webstore.commerce.dto.response.ProductTypeResponse;
import com.webstore.commerce.service.ProductTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/product-types")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class ProductTypeAdminController {

    private final ProductTypeService productTypeService;

    @PostMapping
    public ResponseEntity<ProductTypeResponse> create(@Valid @RequestBody CreateProductTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productTypeService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<ProductTypeResponse>> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(productTypeService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductTypeResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(productTypeService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductTypeResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateProductTypeRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(productTypeService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductTypeResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateProductTypeStatusRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(productTypeService.updateStatus(id, request));
    }
}
