package com.webstore.commerce.entity;

import com.webstore.commerce.entity.enums.KeyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "product_keys",
        indexes = {
                @Index(name = "idx_product_keys_checkout", columnList = "product_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProductKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(name = "key_value_encrypted", nullable = false, columnDefinition = "TEXT")
    private String keyValueEncrypted;

    @Column(name = "key_fingerprint", nullable = false, unique = true, length = 128)
    private String keyFingerprint;

    @Column(name = "cost_price")
    private Long costPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private KeyStatus status;

    @Column(name = "reserved_at")
    private Instant reservedAt;

    @Column(name = "sold_at")
    private Instant soldAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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