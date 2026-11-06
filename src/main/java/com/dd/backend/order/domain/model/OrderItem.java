package com.dd.backend.order.domain.model;

import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.Money;
import java.util.Objects;

/**
 * Item do pedido — snapshot de produto no momento da venda:
 * título e preço ficam congelados mesmo se o catálogo mudar depois.
 */
public final class OrderItem {

    private final Long productId;
    private final String productTitle;
    private final Money unitPrice;
    private final int quantity;

    private OrderItem(Long productId, String productTitle, Money unitPrice, int quantity) {
        if (productId == null) {
            throw new DomainException("Produto do pedido é obrigatório");
        }
        if (productTitle == null || productTitle.isBlank()) {
            throw new DomainException("Título do produto do pedido é obrigatório");
        }
        if (unitPrice == null) {
            throw new DomainException("Preço do item do pedido é obrigatório");
        }
        if (quantity < 1) {
            throw new DomainException("Quantidade do item deve ser maior que zero");
        }
        this.productId = productId;
        this.productTitle = productTitle;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public static OrderItem of(Long productId, String productTitle, Money unitPrice, int quantity) {
        return new OrderItem(productId, productTitle, unitPrice, quantity);
    }

    public Money subtotal() {
        return unitPrice.times(quantity);
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderItem other)) {
            return false;
        }
        return Objects.equals(productId, other.productId)
                && Objects.equals(productTitle, other.productTitle)
                && unitPrice.equals(other.unitPrice)
                && quantity == other.quantity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, productTitle, unitPrice, quantity);
    }
}
