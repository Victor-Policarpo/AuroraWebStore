package com.webstore.commerce.integration.suppliers.mock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockSupplierClientTest {

    @Test
    void mockSuppliersExposeDifferentPricesAndStockIncludingUnavailableOffer() {
        MockSupplierAClient supplierA = new MockSupplierAClient();
        MockSupplierBClient supplierB = new MockSupplierBClient();

        assertEquals("MOCK_A", supplierA.supplierCode());
        assertEquals("MOCK_B", supplierB.supplierCode());
        assertNotEquals(supplierA.fetchOffers().getFirst().wholesalePrice(),
                supplierB.fetchOffers().getFirst().wholesalePrice());
        assertEquals(0, supplierA.fetchOffers().get(1).stockQuantity());
        assertTrue(supplierB.fetchOffers().get(1).stockQuantity() > 0);
    }
}
