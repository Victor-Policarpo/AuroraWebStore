package com.webstore.commerce.mapper;

import com.webstore.commerce.dto.request.CreateProductTypeRequest;
import com.webstore.commerce.dto.request.UpdateProductTypeRequest;
import com.webstore.commerce.dto.response.ProductTypeResponse;
import com.webstore.commerce.entity.ProductType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductTypeMapper {

    ProductType toEntity(CreateProductTypeRequest request);

    ProductTypeResponse toResponse(ProductType productType);

    void updateEntity(UpdateProductTypeRequest request, @MappingTarget ProductType productType);
}
