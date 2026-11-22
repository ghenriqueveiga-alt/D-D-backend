package com.dd.backend.order.application.port.inbound;

import com.dd.backend.order.domain.model.PaymentMethod;
import com.dd.backend.order.domain.model.ShippingAddress;
import java.math.BigDecimal;
import java.util.List;

/**
 * Comando de registro de pedido (checkout).
 *
 * @param customerId    cliente já cadastrado — pode ser {@code null} quando o
 *                      checkout informa {@code customer} (nome + e-mail)
 * @param customer      dados do comprador do checkout (sem cadastro prévio)
 * @param fromCart      {@code true} monta o pedido a partir do carrinho do
 *                      cliente (e o esvazia após a compra); {@code false} usa
 *                      {@code items}
 * @param items         itens explícitos quando {@code fromCart == false}
 * @param address       endereço de entrega (obrigatório)
 * @param shipping      frete em BRL (null = R$ 0,00)
 * @param paymentMethod pagamento escolhido no checkout; ausente deixa o
 *                      pedido PENDING para pagar depois via {@code /pay}
 */
public record PlaceOrderCommand(
        Long customerId,
        CustomerData customer,
        boolean fromCart,
        List<Item> items,
        ShippingAddress address,
        BigDecimal shipping,
        PaymentMethod paymentMethod) {

    /** Dados do comprador enviados pelo checkout. */
    public record CustomerData(String name, String email) {
    }

    public record Item(Long productId, Integer quantity) {
    }
}
