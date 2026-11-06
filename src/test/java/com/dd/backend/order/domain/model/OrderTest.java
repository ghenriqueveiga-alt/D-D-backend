package com.dd.backend.order.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.Money;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regras de negócio do aggregate pedido, sem Spring e sem banco. */
class OrderTest {

    private static OrderItem item(long productId, String title, String price, int quantity) {
        return OrderItem.of(productId, title, Money.of(price), quantity);
    }

    @Test
    void deveSomarTotalDosItensEPercorrenPENDING() {
        Order order = Order.place(10L, List.of(
                item(1L, "Teclado", "100.00", 2),
                item(2L, "Mouse", "50.00", 1)));

        assertEquals(new BigDecimal("250.00"), order.getTotal().amount());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(2, order.getItems().size());
        assertNotNull(order.getCreatedAt());
    }

    @Test
    void pedidoSemItensOuSemClienteERejeitado() {
        assertThrows(DomainException.class, () -> Order.place(10L, List.of()));
        assertThrows(DomainException.class, () -> Order.place(10L, null));
        assertThrows(DomainException.class, () -> Order.place(null, List.of(item(1L, "X", "10.00", 1))));
    }

    @Test
    void itemComQuantidadeInvalidaERejeitado() {
        assertThrows(DomainException.class, () -> OrderItem.of(1L, "X", Money.of("10.00"), 0));
        assertThrows(DomainException.class, () -> OrderItem.of(1L, " ", Money.of("10.00"), 1));
    }

    @Test
    void fluxoCompletoDePagamentoEnvioEEntrega() {
        Order order = Order.place(10L, List.of(item(1L, "X", "10.00", 1)));

        order.pay(PaymentMethod.PIX);
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(PaymentMethod.PIX, order.getPaymentMethod());
        assertNotNull(order.getPaidAt());

        order.ship();
        assertEquals(OrderStatus.SHIPPED, order.getStatus());

        order.deliver();
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void naoPodePagarPedidoJaPago() {
        Order order = Order.place(10L, List.of(item(1L, "X", "10.00", 1)));
        order.pay(PaymentMethod.PIX);

        assertThrows(DomainException.class, () -> order.pay(PaymentMethod.PIX));
    }

    @Test
    void naoPodeEnviarAntesDePagarNemEntregarSemEnvio() {
        Order order = Order.place(10L, List.of(item(1L, "X", "10.00", 1)));

        assertThrows(DomainException.class, order::ship);

        order.pay(PaymentMethod.CREDIT_CARD);
        assertThrows(DomainException.class, order::deliver);
    }

    @Test
    void soPodeCancelarPendenteOuPago() {
        Order pendente = Order.place(10L, List.of(item(1L, "X", "10.00", 1)));
        pendente.cancel();
        assertEquals(OrderStatus.CANCELLED, pendente.getStatus());

        Order pago = Order.place(10L, List.of(item(1L, "X", "10.00", 1)));
        pago.pay(PaymentMethod.BOLETO);
        pago.cancel();
        assertEquals(OrderStatus.CANCELLED, pago.getStatus());

        Order entregue = Order.place(10L, List.of(item(1L, "X", "10.00", 1)));
        entregue.pay(PaymentMethod.PIX);
        entregue.ship();
        entregue.deliver();
        assertThrows(DomainException.class, entregue::cancel);
    }
}
