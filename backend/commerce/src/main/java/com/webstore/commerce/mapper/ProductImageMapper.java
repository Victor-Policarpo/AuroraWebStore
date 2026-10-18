package com.webstore.commerce.mapper;

import com.webstore.commerce.dto.response.ProductImageResponse;
import com.webstore.commerce.entity.Product;
import com.webstore.commerce.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.web.multipart.MultipartFile;


@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductImageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "s3Key", source = "key")
    @Mapping(target = "originalFilename", source = "file.originalFilename")
    @Mapping(target = "contentType", source = "file.contentType")
    @Mapping(target = "fileSize", source = "file.size")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "product", source = "product")
    @Mapping(target = "displayOrder", source = "displayOrder")
    ProductImage toEntity(MultipartFile file, String key, Product product, int displayOrder);

    @Mapping(target = "originalFileName", source = "image.originalFilename")
    @Mapping(target = "url", source = "presignedUrl")
    ProductImageResponse toResponseDto(ProductImage image, String presignedUrl);
}
