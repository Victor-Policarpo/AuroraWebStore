package com.webstore.commerce.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorMessage {
    private String titleError;
    private String contentError;
}
