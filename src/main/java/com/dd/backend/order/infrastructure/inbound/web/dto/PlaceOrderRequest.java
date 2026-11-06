package com.dd.backend.order.infrastructure.inbound.web.dto;

import com.dd.backend.order.application.port.inbound.PlaceOrderCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * Corpo de registro de pedido.
 *
 * <p>{@code fromCart} é {@link Boolean} (e não {@code boolean}) porque campos
 * primitivos não aceitam o {@code null} do Jackson quando a propriedade está
 * ausente no JSON — a ausência aqui significa {@code false}.</p>
 */
public record PlaceOrderRequest(
        @NotNull(message = "customerId é obrigatório") Long customerId,
        Boolean fromCart,
        List<@Valid Item> items) {

    public record Item(
            @NotNull(message = "productId é obrigatório") Long productId,
            @NotNull(message = "quantity é obrigatória")
            @Positive(message = "quantity deve ser maior que zero") Integer quantity) {
    }

    public PlaceOrderCommand toCommand() {
        List<PlaceOrderCommand.Item> commandItems = items == null
                ? null
                : items.stream()
                        .map(item -> new PlaceOrderCommand.Item(item.productId(), item.quantity()))
                        .toList();
        return new PlaceOrderCommand(customerId, Boolean.TRUE.equals(fromCart), commandItems);
    }
}
