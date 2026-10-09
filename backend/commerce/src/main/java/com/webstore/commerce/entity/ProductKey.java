package com.webstore.commerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_keys")
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
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(name = "key_value_encrypted", nullable = false, columnDefinition = "TEXT")
    private String keyValueEncrypted;

    @Column(name = "key_fingerprint", nullable = false, unique = true, length = 128)
    private String keyFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private KeyStatus status;

    @Column(name = "reserved_at")
    private Instant reservedAt;

    @Column(name = "sold_at")
    private Instant soldAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
