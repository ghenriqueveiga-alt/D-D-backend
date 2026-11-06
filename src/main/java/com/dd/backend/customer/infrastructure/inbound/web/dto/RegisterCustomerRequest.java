package com.dd.backend.customer.infrastructure.inbound.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterCustomerRequest(
        @NotBlank(message = "nome é obrigatório") String name,
        @NotBlank(message = "e-mail é obrigatório") @Email(message = "e-mail inválido") String email) {
}
