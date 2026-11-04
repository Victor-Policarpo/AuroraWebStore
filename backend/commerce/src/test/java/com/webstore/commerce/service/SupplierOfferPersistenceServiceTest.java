package com.webstore.commerce.service;

import com.webstore.commerce.entity.Product;
import com.webstore.commerce.entity.Supplier;
import com.webstore.commerce.entity.SupplierOffer;
import com.webstore.commerce.integration.suppliers.dto.SupplierOfferResponse;
import com.webstore.commerce.repository.ProductRepository;
import com.webstore.commerce.repository.SupplierOfferRepository;
import com.webstore.commerce.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierOfferPersistenceServiceTest {

    @Mock private SupplierOfferRepository offerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SupplierRepository supplierRepository;

    @Test
    void createsOfferAssociatedWithProductAndSupplier() {
        SupplierOfferPersistenceService service = service();
        UUID productId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        Product product = product(productId);
        Supplier supplier = supplier(supplierId);
        SupplierOfferResponse response = new SupplierOfferResponse("570", 4500L, "BRL", 12);
        when(offerRepository.findByProductIdAndSupplierId(productId, supplierId)).thenReturn(Optional.empty());
        when(productRepository.getReferenceById(productId)).thenReturn(product);
        when(supplierRepository.getReferenceById(supplierId)).thenReturn(supplier);
        when(offerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(1, service.persistOffers(supplierId, Map.of("570", productId), List.of(response)));

        var captor = org.mockito.ArgumentCaptor.forClass(SupplierOffer.class);
        verify(offerRepository).save(captor.capture());
        assertEquals(product, captor.getValue().getProduct());
        assertEquals(supplier, captor.getValue().getSupplier());
        assertEquals(4500L, captor.getValue().getWholesalePrice());
        assertEquals(12, captor.getValue().getStockQuantity());
    }

    @Test
    void updatesExistingOfferAndAcceptsValidZeroStock() {
        SupplierOfferPersistenceService service = service();
        UUID productId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        SupplierOffer existing = new SupplierOffer();
        existing.setWholesalePrice(2000L);
        existing.setStockQuantity(5);
        SupplierOfferResponse response = new SupplierOfferResponse("730", 3900L, "BRL", 0);
        when(offerRepository.findByProductIdAndSupplierId(productId, supplierId)).thenReturn(Optional.of(existing));
        when(offerRepository.save(existing)).thenReturn(existing);

        assertEquals(1, service.persistOffers(supplierId, Map.of("730", productId), List.of(response)));

        assertEquals(3900L, existing.getWholesalePrice());
        assertEquals(0, existing.getStockQuantity());
        assertEquals("730", existing.getExternalProductId());
        verify(productRepository, never()).getReferenceById(any());
    }

    private SupplierOfferPersistenceService service() {
        return new SupplierOfferPersistenceService(offerRepository, productRepository, supplierRepository);
    }

    private Product product(UUID id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }

    private Supplier supplier(UUID id) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        return supplier;
    }
}
