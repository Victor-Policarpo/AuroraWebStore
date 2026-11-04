package com.webstore.commerce.service;

import com.webstore.commerce.entity.SupplierOffer;
import com.webstore.commerce.integration.suppliers.dto.SupplierOfferResponse;
import com.webstore.commerce.repository.ProductRepository;
import com.webstore.commerce.repository.SupplierOfferRepository;
import com.webstore.commerce.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SupplierOfferPersistenceService {

    private final SupplierOfferRepository supplierOfferRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    public SupplierOfferPersistenceService(
            SupplierOfferRepository supplierOfferRepository,
            ProductRepository productRepository,
            SupplierRepository supplierRepository
    ) {
        this.supplierOfferRepository = supplierOfferRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public int persistOffers(UUID supplierId, Map<String, UUID> productIdsBySteamAppId,
                             List<SupplierOfferResponse> responses) {
        int savedOffers = 0;

        for (SupplierOfferResponse response : responses) {
            UUID productId = productIdsBySteamAppId.get(response.externalProductId());
            if (productId == null) {
                continue;
            }

            SupplierOffer offer = supplierOfferRepository
                    .findByProductIdAndSupplierId(productId, supplierId)
                    .orElseGet(() -> newOffer(productId, supplierId));
            applyResponse(offer, response);
            supplierOfferRepository.save(offer);
            savedOffers++;
        }

        return savedOffers;
    }

    private SupplierOffer newOffer(UUID productId, UUID supplierId) {
        SupplierOffer offer = new SupplierOffer();
        offer.setProduct(productRepository.getReferenceById(productId));
        offer.setSupplier(supplierRepository.getReferenceById(supplierId));
        return offer;
    }

    private void applyResponse(SupplierOffer offer, SupplierOfferResponse response) {
        offer.setExternalProductId(response.externalProductId());
        offer.setWholesalePrice(response.wholesalePrice());
        offer.setWholesaleCurrency(response.wholesaleCurrency());
        offer.setStockQuantity(response.stockQuantity());
        offer.setActive(true);
    }
}
