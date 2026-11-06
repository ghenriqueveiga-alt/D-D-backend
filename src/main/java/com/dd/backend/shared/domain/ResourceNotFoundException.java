package com.dd.backend.shared.domain;

/**
 * Recurso inexistente. Mapeada para HTTP 404 pelo RestExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException(resource + " não encontrado(a): " + id);
    }
}
