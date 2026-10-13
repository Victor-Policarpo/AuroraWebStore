package com.webstore.commerce.mapper;

import com.webstore.commerce.dto.request.CreateCategoryRequest;
import com.webstore.commerce.dto.request.UpdateCategoryRequest;
import com.webstore.commerce.dto.response.CategoryResponse;
import com.webstore.commerce.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {

    Category toEntity(CreateCategoryRequest request);

    CategoryResponse toResponse(Category category);

    void updateEntity(UpdateCategoryRequest request, @MappingTarget Category category);
}
