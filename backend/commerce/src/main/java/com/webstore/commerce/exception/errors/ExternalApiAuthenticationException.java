package com.webstore.commerce.exception.errors;

public class ExternalApiAuthenticationException extends ExternalApiException {

    public ExternalApiAuthenticationException(String message) {
        super(message);
    }

    public ExternalApiAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
