package com.webstore.commerce.repository;

import com.webstore.commerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    long countByProductId(UUID productId);

    List<ProductImage> findByProductId(UUID id);

    @Query("""
        SELECT MAX(pi.displayOrder)
        FROM ProductImage pi
        WHERE pi.product.id = :productId
    """)
    Integer findMaxDisplayOrderByProductId(
            @Param("productId") UUID productId
    );
}
