package com.dd.backend.cart.infrastructure.inbound.web.dto;

import com.dd.backend.cart.application.port.inbound.CartView;
import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long customerId, List<Item> items, BigDecimal total) {

    public record Item(Long productId, String title, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
    }

    public static CartResponse from(CartView view) {
        List<Item> items = view.items().stream()
                .map(item -> new Item(
                        item.productId(),
                        item.title(),
                        item.unitPrice().amount(),
                        item.quantity(),
                        item.subtotal().amount()))
                .toList();
        return new CartResponse(view.customerId(), items, view.total().amount());
    }
}
