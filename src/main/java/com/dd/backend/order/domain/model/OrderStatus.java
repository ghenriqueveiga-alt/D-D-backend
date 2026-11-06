package com.dd.backend.order.domain.model;

/** Ciclo de vida do pedido. */
public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
