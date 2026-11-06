package com.dd.backend.customer.domain.repository;

import com.dd.backend.customer.domain.model.Customer;
import java.util.List;
import java.util.Optional;

/** Porta de saída (hexagonal) de persistência de clientes. */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(Long id);

    Optional<Customer> findByEmail(String email);

    List<Customer> findAll();
}
