package com.dd.backend.cart.application.port.inbound;

/** Porta de entrada: esvazia o carrinho do cliente (idempotente). */
public interface ClearCartUseCase {

    void clear(Long customerId);
}
