package com.dd.backend.cart.application.port.inbound;

/** Porta de entrada: adiciona um item ao carrinho do cliente. */
public interface AddCartItemUseCase {

    CartView add(Long customerId, Long productId, int quantity);
}
