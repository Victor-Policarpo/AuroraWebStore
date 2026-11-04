package com.webstore.commerce.service;

public record SupplierSyncFailure(String supplierCode, Type type, String message) {

    public enum Type {
        COMMUNICATION,
        INVALID_RESPONSE
    }
}
