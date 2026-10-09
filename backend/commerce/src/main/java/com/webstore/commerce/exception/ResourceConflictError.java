package com.webstore.commerce.exception;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ResourceConflictError extends StandardError {
    private final List<ResourceConflict> conflicts = new ArrayList<>();
    public void addConflict(ResourceConflict conflict){
        conflicts.add(conflict);
    }
}