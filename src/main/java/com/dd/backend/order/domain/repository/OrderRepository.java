package com.dd.backend.order.domain.repository;

import com.dd.backend.order.domain.model.Order;
import java.util.List;
import java.util.Optional;

/** Porta de saída (hexagonal) de persistência de pedidos. */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    List<Order> findAll();

    List<Order> findByCustomerId(Long customerId);
}
