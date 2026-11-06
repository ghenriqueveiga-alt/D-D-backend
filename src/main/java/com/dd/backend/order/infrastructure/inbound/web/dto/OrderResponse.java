package com.dd.backend.order.infrastructure.inbound.web.dto;

import com.dd.backend.order.domain.model.Order;
import com.dd.backend.order.domain.model.OrderStatus;
import com.dd.backend.order.domain.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal total,
        Instant createdAt,
        Instant paidAt,
        List<Item> items) {

    public record Item(Long productId, String productTitle, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
    }

    public static OrderResponse from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(item -> new Item(
                        item.getProductId(),
                        item.getProductTitle(),
                        item.getUnitPrice().amount(),
                        item.getQuantity(),
                        item.subtotal().amount()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTotal().amount(),
                order.getCreatedAt(),
                order.getPaidAt(),
                items);
    }
}
