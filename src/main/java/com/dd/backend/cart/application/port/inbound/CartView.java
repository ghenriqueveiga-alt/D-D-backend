package com.dd.backend.cart.application.port.inbound;

import com.dd.backend.shared.domain.Money;
import java.util.List;

/** Resultado da consulta ao carrinho, enriquecido com dados do catálogo. */
public record CartView(Long customerId, List<CartItemView> items, Money total) {

    public record CartItemView(
            Long productId,
            String title,
            Money unitPrice,
            int quantity,
            Money subtotal) {
    }
}
