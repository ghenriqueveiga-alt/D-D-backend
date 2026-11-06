package com.dd.backend.customer.application.port.inbound;

import com.dd.backend.customer.domain.model.Customer;

/** Porta de entrada: busca de cliente pelo id. */
public interface GetCustomerUseCase {

    Customer execute(Long customerId);
}
