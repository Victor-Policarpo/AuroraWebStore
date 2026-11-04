package com.webstore.commerce.service;

import java.util.List;

public record SupplierSyncResult(int offersSaved, List<SupplierSyncFailure> failures) {

    public SupplierSyncResult {
        failures = List.copyOf(failures);
    }
}
