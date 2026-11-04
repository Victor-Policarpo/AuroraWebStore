package com.webstore.commerce.integration.suppliers.mock;

import com.webstore.commerce.integration.suppliers.client.SupplierClient;
import com.webstore.commerce.integration.suppliers.dto.SupplierOfferResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockSupplierAClient implements SupplierClient {

    private static final List<SupplierOfferResponse> OFFERS = List.of(
            new SupplierOfferResponse("570", 4_500L, "BRL", 12),
            new SupplierOfferResponse("730", 3_900L, "BRL", 0)
    );

    @Override
    public String supplierCode() {
        return "MOCK_A";
    }

    @Override
    public List<SupplierOfferResponse> fetchOffers() {
        return OFFERS;
    }
}
