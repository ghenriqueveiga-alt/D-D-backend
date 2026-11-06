package com.dd.backend.shared.domain;

/**
 * Falha de integração com um serviço externo (ex.: DummyJSON).
 * Mapeada para HTTP 502 pelo RestExceptionHandler.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
