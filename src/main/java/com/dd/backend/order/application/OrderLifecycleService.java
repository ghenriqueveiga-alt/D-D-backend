package com.dd.backend.order.application;

import com.dd.backend.order.application.port.inbound.OrderLifecycleUseCase;
import com.dd.backend.order.domain.model.Order;
import com.dd.backend.order.domain.model.PaymentMethod;
import com.dd.backend.order.domain.repository.OrderRepository;
import com.dd.backend.shared.domain.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Transições de estado do pedido — toda a regra fica no aggregate {@link Order}.
 */
@Service
@RequiredArgsConstructor
public class OrderLifecycleService implements OrderLifecycleUseCase {

    private final OrderRepository orderRepository;

    @Override
    public Order pay(Long orderId, PaymentMethod method) {
        Order order = load(orderId);
        order.pay(method);
        return orderRepository.save(order);
    }

    @Override
    public Order ship(Long orderId) {
        Order order = load(orderId);
        order.ship();
        return orderRepository.save(order);
    }

    @Override
    public Order deliver(Long orderId) {
        Order order = load(orderId);
        order.deliver();
        return orderRepository.save(order);
    }

    @Override
    public Order cancel(Long orderId) {
        Order order = load(orderId);
        order.cancel();
        return orderRepository.save(order);
    }

    private Order load(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Pedido", orderId));
    }
}
