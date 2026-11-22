package com.dd.backend.order.infrastructure.inbound.web.dto;

import com.dd.backend.order.application.port.inbound.PlaceOrderCommand;
import com.dd.backend.order.domain.model.PaymentMethod;
import com.dd.backend.order.domain.model.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * Corpo de registro de pedido (checkout).
 *
 * <p>Identificação do comprador: informe {@code customerId} (já cadastrado) ou
 * {@code customer} com nome + e-mail — no segundo caso o backend localiza o
 * cliente pelo e-mail ou cria o cadastro.</p>
 *
 * <p>{@code fromCart} é {@link Boolean} (e não {@code boolean}) porque campos
 * primitivos não aceitam o {@code null} do Jackson quando a propriedade está
 * ausente no JSON — a ausência aqui significa {@code false}.</p>
 */
public record PlaceOrderRequest(
        Long customerId,
        @Valid CustomerPayload customer,
        Boolean fromCart,
        List<@Valid Item> items,
        @NotNull(message = "endereço de entrega é obrigatório") @Valid AddressPayload address,
        BigDecimal shipping,
        PaymentMethod paymentMethod) {

    public record CustomerPayload(
            @NotBlank(message = "nome do cliente é obrigatório") String name,
            @NotBlank(message = "e-mail do cliente é obrigatório") String email) {
    }

    public record Item(
            @NotNull(message = "productId é obrigatório") Long productId,
            @NotNull(message = "quantity é obrigatória")
            @Positive(message = "quantity deve ser maior que zero") Integer quantity) {
    }

    public record AddressPayload(
            @NotBlank(message = "nome do destinatário é obrigatório") String recipientName,
            @NotBlank(message = "telefone é obrigatório") String phone,
            @NotBlank(message = "CEP é obrigatório") String cep,
            @NotBlank(message = "rua é obrigatória") String street,
            @NotBlank(message = "número é obrigatório") String number,
            String complement,
            @NotBlank(message = "cidade é obrigatória") String city,
            @NotBlank(message = "UF é obrigatória") String uf) {
    }

    public PlaceOrderCommand toCommand() {
        PlaceOrderCommand.CustomerData customerData = customer == null
                ? null
                : new PlaceOrderCommand.CustomerData(customer.name(), customer.email());
        List<PlaceOrderCommand.Item> commandItems = items == null
                ? null
                : items.stream()
                        .map(item -> new PlaceOrderCommand.Item(item.productId(), item.quantity()))
                        .toList();
        ShippingAddress shippingAddress = new ShippingAddress(
                address.recipientName(),
                address.phone(),
                address.cep(),
                address.street(),
                address.number(),
                address.complement(),
                address.city(),
                address.uf());
        return new PlaceOrderCommand(
                customerId, customerData, Boolean.TRUE.equals(fromCart), commandItems,
                shippingAddress, shipping, paymentMethod);
    }
}
