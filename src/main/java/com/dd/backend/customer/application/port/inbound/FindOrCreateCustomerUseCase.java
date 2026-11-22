package com.dd.backend.customer.application.port.inbound;

import com.dd.backend.customer.domain.model.Customer;

/**
 * Porta de entrada: localiza o cliente pelo e-mail ou o registra caso ainda
 * não exista — usada pelo checkout, onde o comprador não tem cadastro prévio.
 */
public interface FindOrCreateCustomerUseCase {

    /** Devolve o cliente existente com o e-mail ou cria um novo. */
    Customer findOrCreate(String name, String email);
}
