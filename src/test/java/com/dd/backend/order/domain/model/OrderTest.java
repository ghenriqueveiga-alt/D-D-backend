package com.dd.backend.order.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regras de negócio do aggregate pedido, sem Spring e sem banco. */
class OrderTest {

    private static OrderItem item(long productId, String title, String price, int quantity) {
        return OrderItem.of(productId, title, Money.of(price), quantity);
    }

    private static ShippingAddress address() {
        return ShippingAddress.of(
                "Maria da Silva", "11 99999-9999", "01310-100",
                "Avenida Paulista", "1000", "apto 42", "São Paulo", "SP");
    }

    private static Order place(long customerId) {
        return Order.place(customerId, List.of(item(1L, "X", "10.00", 1)), address(), Money.zero());
    }

    @Test
    void deveSomarTotalDosItensEPercorrenPENDING() {
        Order order = Order.place(10L, List.of(
                item(1L, "Teclado", "100.00", 2),
                item(2L, "Mouse", "50.00", 1)), address(), Money.zero());

        assertEquals(new BigDecimal("250.00"), order.getTotal().amount());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(2, order.getItems().size());
        assertNotNull(order.getCreatedAt());
        assertEquals("01310-100", order.getAddress().cep());
        assertEquals(new BigDecimal("0.00"), order.getShipping().amount());
    }

    @Test
    void freteEntraNoTotalDoPedido() {
        Order order = Order.place(10L, List.of(item(1L, "Teclado", "100.00", 1)),
                address(), Money.of("19.90"));

        assertEquals(new BigDecimal("19.90"), order.getShipping().amount());
        assertEquals(new BigDecimal("119.90"), order.getTotal().amount());
    }

    @Test
    void pedidoSemItensOuSemClienteOuSemEnderecoERejeitado() {
        assertThrows(DomainException.class,
                () -> Order.place(10L, List.of(), address(), Money.zero()));
        assertThrows(DomainException.class,
                () -> Order.place(10L, null, address(), Money.zero()));
        assertThrows(DomainException.class,
                () -> Order.place(null, List.of(item(1L, "X", "10.00", 1)), address(), Money.zero()));
        assertThrows(DomainException.class,
                () -> Order.place(10L, List.of(item(1L, "X", "10.00", 1)), null, Money.zero()));
    }

    @Test
    void itemComQuantidadeInvalidaERejeitado() {
        assertThrows(DomainException.class, () -> OrderItem.of(1L, "X", Money.of("10.00"), 0));
        assertThrows(DomainException.class, () -> OrderItem.of(1L, " ", Money.of("10.00"), 1));
    }

    @Test
    void fluxoCompletoDePagamentoEnvioEEntrega() {
        Order order = place(10L);

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
        Order order = place(10L);
        order.pay(PaymentMethod.PIX);

        assertThrows(DomainException.class, () -> order.pay(PaymentMethod.PIX));
    }

    @Test
    void naoPodeEnviarAntesDePagarNemEntregarSemEnvio() {
        Order order = place(10L);

        assertThrows(DomainException.class, order::ship);

        order.pay(PaymentMethod.CREDIT_CARD);
        assertThrows(DomainException.class, order::deliver);
    }

    @Test
    void soPodeCancelarPendenteOuPago() {
        Order pendente = place(10L);
        pendente.cancel();
        assertEquals(OrderStatus.CANCELLED, pendente.getStatus());

        Order pago = place(10L);
        pago.pay(PaymentMethod.BOLETO);
        pago.cancel();
        assertEquals(OrderStatus.CANCELLED, pago.getStatus());

        Order entregue = place(10L);
        entregue.pay(PaymentMethod.PIX);
        entregue.ship();
        entregue.deliver();
        assertThrows(DomainException.class, entregue::cancel);
    }

    @Test
    void restoreDePedidoLegadoAceitaEndENuloEFreteNulo() {
        Order legacy = Order.restore(
                7L, 10L, List.of(item(1L, "X", "10.00", 2)),
                OrderStatus.DELIVERED, PaymentMethod.BOLETO,
                Instant.parse("2026-01-01T00:00:00Z"), null,
                null, null);

        assertNull(legacy.getAddress());
        assertEquals(new BigDecimal("0.00"), legacy.getShipping().amount());
        assertEquals(new BigDecimal("20.00"), legacy.getTotal().amount());
    }
}
