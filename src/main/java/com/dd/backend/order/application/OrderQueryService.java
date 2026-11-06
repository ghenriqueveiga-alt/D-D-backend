package com.dd.backend.order.application;

import com.dd.backend.order.application.port.inbound.OrderQueryUseCase;
import com.dd.backend.order.domain.model.Order;
import com.dd.backend.order.domain.repository.OrderRepository;
import com.dd.backend.shared.domain.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderQueryService implements OrderQueryUseCase {

    private final OrderRepository orderRepository;

    @Override
    public Order get(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Pedido", orderId));
    }

    @Override
    public List<Order> list() {
        return orderRepository.findAll();
    }

    @Override
    public List<Order> listByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }
}
