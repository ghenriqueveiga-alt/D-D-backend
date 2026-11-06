package com.dd.backend.cart.domain.model;

import com.dd.backend.shared.domain.DomainException;

/** Item do carrinho (parte do aggregate {@link Cart}). */
public final class CartItem {

    private final Long productId;
    private int quantity;

    private CartItem(Long productId, int quantity) {
        if (productId == null) {
            throw new DomainException("Produto do carrinho é obrigatório");
        }
        if (quantity < 1) {
            throw new DomainException("Quantidade deve ser maior que zero");
        }
        this.productId = productId;
        this.quantity = quantity;
    }

    public static CartItem of(Long productId, int quantity) {
        return new CartItem(productId, quantity);
    }

    void increase(int quantity) {
        if (quantity < 1) {
            throw new DomainException("Quantidade deve ser maior que zero");
        }
        this.quantity += quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
