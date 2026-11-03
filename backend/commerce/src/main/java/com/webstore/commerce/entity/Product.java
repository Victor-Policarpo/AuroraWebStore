package com.webstore.commerce.entity;

import com.webstore.commerce.entity.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new LinkedHashSet<>();

    @Transient
    public Category getCategory() { return categories.stream().findFirst().orElse(null); }

    @Transient
    public void setCategory(Category category) {
        categories.clear();
        if (category != null) categories.add(category);
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_type_id", nullable = false)
    private ProductType productType;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "trailer_url", length = 500)
    private String trailerUrl;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "platform", length = 100)
    private String platform;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "base_price")
    private Long basePrice;

    @Column(name = "base_currency", length = 3)
    private String baseCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "igdb_id", unique = true, nullable = false)
    private Long igdbId;

    @Column(name = "steam_appid", unique = true)
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
