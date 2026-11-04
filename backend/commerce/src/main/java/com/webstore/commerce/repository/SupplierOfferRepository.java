package com.webstore.commerce.repository;

import com.webstore.commerce.entity.SupplierOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupplierOfferRepository extends JpaRepository<SupplierOffer, UUID> {

    Optional<SupplierOffer> findByProductIdAndSupplierId(UUID productId, UUID supplierId);
}
