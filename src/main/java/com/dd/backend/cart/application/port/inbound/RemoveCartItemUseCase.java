package com.dd.backend.cart.application.port.inbound;

/** Porta de entrada: remove um item do carrinho do cliente. */
public interface RemoveCartItemUseCase {

    CartView remove(Long customerId, Long productId);
}
