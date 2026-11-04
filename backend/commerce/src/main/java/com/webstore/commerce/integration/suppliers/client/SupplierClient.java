package com.webstore.commerce.integration.suppliers.client;

import com.webstore.commerce.exception.errors.SupplierCommunicationException;
import com.webstore.commerce.integration.suppliers.dto.SupplierOfferResponse;

import java.util.List;

public interface SupplierClient {

    String supplierCode();

    /**
     * @throws SupplierCommunicationException when an expected supplier communication failure occurs
     */
    List<SupplierOfferResponse> fetchOffers();
}
