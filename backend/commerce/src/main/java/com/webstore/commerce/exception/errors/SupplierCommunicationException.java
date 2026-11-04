package com.webstore.commerce.exception.errors;

public class SupplierCommunicationException extends RuntimeException {

    public SupplierCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SupplierCommunicationException(String message) {
        super(message);
    }
}
