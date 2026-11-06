package com.dd.backend.customer.application.port.inbound;

import com.dd.backend.customer.domain.model.Customer;

/** Porta de entrada: cadastro de cliente. */
public interface RegisterCustomerUseCase {

    Customer execute(String name, String email);
}
