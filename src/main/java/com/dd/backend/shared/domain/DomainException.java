package com.dd.backend.shared.domain;

/**
 * Violação de regra de negócio. Mapeada para HTTP 422 pelo RestExceptionHandler.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
