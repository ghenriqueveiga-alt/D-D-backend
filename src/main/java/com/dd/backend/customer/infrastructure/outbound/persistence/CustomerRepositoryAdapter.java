package com.dd.backend.customer.infrastructure.outbound.persistence;

import com.dd.backend.customer.domain.model.Customer;
import com.dd.backend.customer.domain.repository.CustomerRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpaRepository;

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity saved = jpaRepository.save(toEntity(customer));
        if (customer.getId() == null) {
            customer.assignId(saved.getId());
        }
        return customer;
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public List<Customer> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private CustomerJpaEntity toEntity(Customer customer) {
        CustomerJpaEntity entity = new CustomerJpaEntity();
        entity.setId(customer.getId());
        entity.setName(customer.getName());
        entity.setEmail(customer.getEmail());
        entity.setCreatedAt(customer.getCreatedAt());
        return entity;
    }

    private Customer toDomain(CustomerJpaEntity entity) {
        return Customer.restore(entity.getId(), entity.getName(), entity.getEmail(), entity.getCreatedAt());
    }
}
