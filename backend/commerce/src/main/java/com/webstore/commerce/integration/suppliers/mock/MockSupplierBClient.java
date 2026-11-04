package com.webstore.commerce.integration.suppliers.mock;

import com.webstore.commerce.integration.suppliers.client.SupplierClient;
import com.webstore.commerce.integration.suppliers.dto.SupplierOfferResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockSupplierBClient implements SupplierClient {

    private static final List<SupplierOfferResponse> OFFERS = List.of(
            new SupplierOfferResponse("570", 5_200L, "BRL", 3),
            new SupplierOfferResponse("730", 4_600L, "BRL", 8)
    );

    @Override
    public String supplierCode() {
        return "MOCK_B";
    }

    @Override
    public List<SupplierOfferResponse> fetchOffers() {
        return OFFERS;
    }
}
