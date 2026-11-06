package com.dd.backend.order.application.port.inbound;

import java.util.List;

/**
 * Comando de registro de pedido.
 *
 * @param customerId cliente que está comprando
 * @param fromCart   {@code true} monta o pedido a partir do carrinho do cliente
 *                   (e o esvazia após a compra); {@code false} usa {@code items}
 * @param items      itens explícitos quando {@code fromCart == false}
 */
public record PlaceOrderCommand(Long customerId, boolean fromCart, List<Item> items) {

    public record Item(Long productId, Integer quantity) {
    }
}
