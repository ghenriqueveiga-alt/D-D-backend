package com.dd.backend.customer.infrastructure.inbound.web.dto;

import com.dd.backend.customer.domain.model.Customer;
import java.time.Instant;

public record CustomerResponse(Long id, String name, String email, Instant createdAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail(), customer.getCreatedAt());
    }
}
