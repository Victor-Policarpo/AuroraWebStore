package com.webstore.commerce.controller;

import com.webstore.commerce.dto.request.CreateCategoryRequest;
import com.webstore.commerce.dto.request.UpdateCategoryRequest;
import com.webstore.commerce.dto.request.UpdateCategoryStatusRequest;
import com.webstore.commerce.dto.response.CategoryResponse;
import com.webstore.commerce.service.CategoryService;
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
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class CategoryAdminController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.findById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CategoryResponse> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateCategoryStatusRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.updateStatus(id, request));
    }
}
