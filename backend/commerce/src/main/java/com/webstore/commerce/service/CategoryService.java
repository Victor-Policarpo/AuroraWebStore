package com.webstore.commerce.service;

import com.webstore.commerce.dto.request.CreateCategoryRequest;
import com.webstore.commerce.dto.request.UpdateCategoryRequest;
import com.webstore.commerce.dto.request.UpdateCategoryStatusRequest;
import com.webstore.commerce.dto.response.CategoryResponse;
import com.webstore.commerce.entity.Category;
import com.webstore.commerce.exception.errors.ResourceAlreadyExistsException;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.mapper.CategoryMapper;
import com.webstore.commerce.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new ResourceAlreadyExistsException("Category with this name already exists");
        }
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new ResourceAlreadyExistsException("Category with this slug already exists");
        }
        Category category = categoryMapper.toEntity(request);
        category.setActive(true);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    public Page<CategoryResponse> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(categoryMapper::toResponse);
    }

    public CategoryResponse findById(UUID id) {
        return categoryMapper.toResponse(getCategory(id));
    }

    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        Category category = getCategory(id);
        if (request.name() != null && !request.name().equals(category.getName())
                && categoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ResourceAlreadyExistsException("Category with this name already exists");
        }
        if (request.slug() != null && !request.slug().equals(category.getSlug())
                && categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new ResourceAlreadyExistsException("Category with this slug already exists");
        }
        categoryMapper.updateEntity(request, category);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateStatus(UUID id, UpdateCategoryStatusRequest request) {
        Category category = getCategory(id);
        category.setActive(request.active());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    private Category getCategory(UUID id) {
        return categoryRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Category not found")
        );
    }
}
