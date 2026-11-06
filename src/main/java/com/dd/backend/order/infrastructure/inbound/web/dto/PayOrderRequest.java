package com.dd.backend.order.infrastructure.inbound.web.dto;

import com.dd.backend.order.domain.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PayOrderRequest(
        @NotNull(message = "paymentMethod é obrigatório") PaymentMethod paymentMethod) {
}
