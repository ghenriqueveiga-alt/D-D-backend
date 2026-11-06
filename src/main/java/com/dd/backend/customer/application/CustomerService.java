package com.dd.backend.customer.application;

import com.dd.backend.customer.application.port.inbound.GetCustomerUseCase;
import com.dd.backend.customer.application.port.inbound.ListCustomersUseCase;
import com.dd.backend.customer.application.port.inbound.RegisterCustomerUseCase;
import com.dd.backend.customer.domain.model.Customer;
import com.dd.backend.customer.domain.repository.CustomerRepository;
import com.dd.backend.shared.domain.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Casos de uso do contexto de clientes. */
@Service
@RequiredArgsConstructor
public class CustomerService implements RegisterCustomerUseCase, GetCustomerUseCase, ListCustomersUseCase {

    private final CustomerRepository customerRepository;

    @Override
    public Customer execute(String name, String email) {
        return customerRepository.save(Customer.register(name, email));
    }

    @Override
    public Customer execute(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", customerId));
    }

    @Override
    public List<Customer> execute() {
        return customerRepository.findAll();
    }
}
