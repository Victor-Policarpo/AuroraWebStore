package com.webstore.commerce.entity;

import com.webstore.commerce.entity.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "platform", nullable = false, length = 100)
    private String platform;

    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Column(name = "base_price", nullable = false)
    private Long basePrice;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "igdb_id", unique = true)
    private Long igdbId;

    @Column(name = "steam_appid")
    private Long steamAppId;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
