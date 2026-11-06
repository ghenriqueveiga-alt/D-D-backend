package com.dd.backend.customer.application.port.inbound;

import com.dd.backend.customer.domain.model.Customer;
import java.util.List;

/** Porta de entrada: listagem de clientes. */
public interface ListCustomersUseCase {

    List<Customer> execute();
}
