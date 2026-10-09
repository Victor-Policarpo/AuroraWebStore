package com.webstore.commerce.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResourceConflict  {
    private String titleError;
    private String contentError;
    private Long id;
    private String name;
}
