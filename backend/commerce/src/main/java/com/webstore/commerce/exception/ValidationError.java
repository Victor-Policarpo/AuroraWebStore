package com.webstore.commerce.exception;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ValidationError extends StandardError {
    private final List<ErrorMessage> errors = new ArrayList<>();
    public void addError(String fieldName, String message){
        errors.add(new ErrorMessage(fieldName, message));
    }
}