package com.webstore.commerce.mapper;

import com.webstore.commerce.dto.request.CreateProductRequest;
import com.webstore.commerce.dto.request.UpdateProductRequest;
import com.webstore.commerce.dto.response.ProductResponse;
import com.webstore.commerce.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "productType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(CreateProductRequest request);

    @Mapping(target = "categoryIds", expression = "java(product.getCategories().stream().map(com.webstore.commerce.entity.Category::getId).toList())")
    @Mapping(target = "productTypeId", source = "productType.id")
    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "productType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateProductRequest request, @MappingTarget Product product);
}
