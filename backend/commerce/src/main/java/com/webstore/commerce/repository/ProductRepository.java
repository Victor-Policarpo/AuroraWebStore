package com.webstore.commerce.repository;

import com.webstore.commerce.entity.Product;
import com.webstore.commerce.entity.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByIgdbId(Long igdbId);

    boolean existsByIgdbIdAndIdNot(Long igdbId, UUID id);

    @Query("""
            SELECT p FROM Product p
            WHERE (:status IS NULL OR p.status = :status)
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
            """)
    Page<Product> search(@Param("status") ProductStatus status,
                         @Param("categoryId") UUID categoryId,
                         Pageable pageable);
}