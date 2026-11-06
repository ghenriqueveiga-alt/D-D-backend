package com.dd.backend.cart.infrastructure.inbound.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull(message = "productId é obrigatório") Long productId,
        @NotNull(message = "quantity é obrigatória") @Positive(message = "quantity deve ser maior que zero") Integer quantity) {
}
