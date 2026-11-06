package com.dd.backend.cart.application.port.inbound;

/** Porta de entrada: consulta do carrinho do cliente. */
public interface GetCartUseCase {

    CartView get(Long customerId);
}
