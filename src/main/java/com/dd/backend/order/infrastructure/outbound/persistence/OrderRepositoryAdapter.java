package com.dd.backend.order.infrastructure.outbound.persistence;

import com.dd.backend.order.domain.model.Order;
import com.dd.backend.order.domain.model.OrderItem;
import com.dd.backend.order.domain.model.ShippingAddress;
import com.dd.backend.order.domain.repository.OrderRepository;
import com.dd.backend.shared.domain.Money;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    @Override
    public Order save(Order order) {
        OrderJpaEntity saved = jpaRepository.save(toEntity(order));
        if (order.getId() == null) {
            order.assignId(saved.getId());
        }
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Order> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findByCustomerId(Long customerId) {
        return jpaRepository.findByCustomerId(customerId).stream().map(this::toDomain).toList();
    }

    private OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.getId());
        entity.setCustomerId(order.getCustomerId());
        entity.setStatus(order.getStatus());
        entity.setTotal(order.getTotal().amount());
        entity.setShipping(order.getShipping().amount());
        entity.setPaymentMethod(order.getPaymentMethod());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setPaidAt(order.getPaidAt());
        if (order.getAddress() != null) {
            entity.setRecipientName(order.getAddress().recipientName());
            entity.setPhone(order.getAddress().phone());
            entity.setCep(order.getAddress().cep());
            entity.setStreet(order.getAddress().street());
            entity.setAddressNumber(order.getAddress().number());
            entity.setComplement(order.getAddress().complement());
            entity.setCity(order.getAddress().city());
            entity.setUf(order.getAddress().uf());
        }
        entity.setItems(order.getItems().stream().map(this::toEntity).toList());
        return entity;
    }

    private OrderItemJpaEntity toEntity(OrderItem item) {
        OrderItemJpaEntity entity = new OrderItemJpaEntity();
        entity.setProductId(item.getProductId());
        entity.setProductTitle(item.getProductTitle());
        entity.setUnitPrice(item.getUnitPrice().amount());
        entity.setQuantity(item.getQuantity());
        return entity;
    }

    private Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(item -> OrderItem.of(
                        item.getProductId(),
                        item.getProductTitle(),
                        Money.of(item.getUnitPrice()),
                        item.getQuantity()))
                .toList();
        return Order.restore(
                entity.getId(),
                entity.getCustomerId(),
                items,
                entity.getStatus(),
                entity.getPaymentMethod(),
                entity.getCreatedAt(),
                entity.getPaidAt(),
                toAddress(entity),
                entity.getShipping() == null ? null : Money.of(entity.getShipping()));
    }

    /** Pedidos anteriores ao checkout não têm endereço gravado. */
    private ShippingAddress toAddress(OrderJpaEntity entity) {
        if (entity.getRecipientName() == null) {
            return null;
        }
        return new ShippingAddress(
                entity.getRecipientName(),
                entity.getPhone(),
                entity.getCep(),
                entity.getStreet(),
                entity.getAddressNumber(),
                entity.getComplement(),
                entity.getCity(),
                entity.getUf());
    }
}
