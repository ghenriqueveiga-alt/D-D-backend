package com.dd.backend.order.domain.model;

import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.Money;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate pedido (raiz de agregação da venda).
 * Toda a transição de estado passa por regras de negócio aqui.
 */
public final class Order {

    private Long id;
    private Long customerId;
    private List<OrderItem> items;
    private OrderStatus status;
    private Money total;
    private Money shipping;
    private ShippingAddress address;
    private PaymentMethod paymentMethod;
    private Instant createdAt;
    private Instant paidAt;

    private Order() {
    }

    /**
     * Registra um novo pedido em PENDING com o total somado dos itens mais o
     * frete. O endereço de entrega é obrigatório (todo pedido nasce do fluxo
     * de checkout).
     */
    public static Order place(Long customerId, List<OrderItem> items, ShippingAddress address, Money shipping) {
        if (address == null) {
            throw new DomainException("Endereço de entrega é obrigatório");
        }
        return assemble(customerId, items, address, shipping);
    }

    /**
     * Reconstituição a partir do banco (uso exclusivo da camada de
     * persistência). Pedidos antigos (anteriores ao checkout com endereço)
     * podem ter {@code address == null}.
     */
    public static Order restore(Long id, Long customerId, List<OrderItem> items, OrderStatus status,
                                PaymentMethod paymentMethod, Instant createdAt, Instant paidAt,
                                ShippingAddress address, Money shipping) {
        Order order = assemble(customerId, items, address, shipping);
        order.id = Objects.requireNonNull(id, "id é obrigatório na reconstituição");
        order.status = Objects.requireNonNull(status, "status é obrigatório na reconstituição");
        order.paymentMethod = paymentMethod;
        order.createdAt = Objects.requireNonNull(createdAt, "createdAt é obrigatório na reconstituição");
        order.paidAt = paidAt;
        return order;
    }

    /** Monta o aggregate com o total = itens + frete (regras de pedido em comum). */
    private static Order assemble(Long customerId, List<OrderItem> items, ShippingAddress address, Money shipping) {
        if (customerId == null) {
            throw new DomainException("Cliente do pedido é obrigatório");
        }
        if (items == null || items.isEmpty()) {
            throw new DomainException("Pedido deve conter ao menos um item");
        }
        Order order = new Order();
        order.customerId = customerId;
        order.items = new ArrayList<>(items);
        order.address = address;
        order.shipping = shipping == null ? Money.zero() : shipping;
        order.status = OrderStatus.PENDING;
        order.total = items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.zero(), Money::plus)
                .plus(order.shipping);
        order.createdAt = Instant.now();
        return order;
    }

    /** PENDING → PAID. */
    public void pay(PaymentMethod method) {
        if (method == null) {
            throw new DomainException("Forma de pagamento é obrigatória");
        }
        requireStatus(OrderStatus.PENDING, "Apenas pedidos pendentes podem ser pagos");
        this.status = OrderStatus.PAID;
        this.paymentMethod = method;
        this.paidAt = Instant.now();
    }

    /** PAID → SHIPPED. */
    public void ship() {
        requireStatus(OrderStatus.PAID, "Apenas pedidos pagos podem ser enviados");
        this.status = OrderStatus.SHIPPED;
    }

    /** SHIPPED → DELIVERED. */
    public void deliver() {
        requireStatus(OrderStatus.SHIPPED, "Apenas pedidos enviados podem ser entregues");
        this.status = OrderStatus.DELIVERED;
    }

    /** PENDING/PAID → CANCELLED. */
    public void cancel() {
        if (status != OrderStatus.PENDING && status != OrderStatus.PAID) {
            throw new DomainException(
                    "Apenas pedidos pendentes ou pagos podem ser cancelados (status atual: " + status + ")");
        }
        this.status = OrderStatus.CANCELLED;
    }

    private void requireStatus(OrderStatus expected, String message) {
        if (status != expected) {
            throw new DomainException(message + " (status atual: " + status + ")");
        }
    }

    /** Atribuído pelo adapter de persistência após o INSERT. */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new DomainException("ID do pedido já foi atribuído");
        }
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotal() {
        return total;
    }

    public Money getShipping() {
        return shipping;
    }

    public ShippingAddress getAddress() {
        return address;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }
}
