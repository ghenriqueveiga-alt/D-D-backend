package com.dd.backend.cart.domain.model;

import com.dd.backend.shared.domain.DomainException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate carrinho: um carrinho por cliente, com itens (produto + quantidade).
 * O preço é resolvido no checkout, a partir do catálogo.
 */
public final class Cart {

    private Long id;
    private Long customerId;
    private final List<CartItem> items = new ArrayList<>();

    private Cart() {
    }

    public static Cart openFor(Long customerId) {
        if (customerId == null) {
            throw new DomainException("Cliente do carrinho é obrigatório");
        }
        Cart cart = new Cart();
        cart.customerId = customerId;
        return cart;
    }

    /** Reconstituição a partir do banco (uso exclusivo da camada de persistência). */
    public static Cart restore(Long id, Long customerId, List<CartItem> items) {
        Cart cart = openFor(customerId);
        cart.id = Objects.requireNonNull(id, "id é obrigatório na reconstituição");
        if (items != null) {
            cart.items.addAll(items);
        }
        return cart;
    }

    /** Adiciona o item; se o produto já existir, soma a quantidade. */
    public void addItem(Long productId, int quantity) {
        items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.increase(quantity),
                        () -> items.add(CartItem.of(productId, quantity)));
    }

    public void removeItem(Long productId) {
        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new DomainException("Produto não está no carrinho: " + productId);
        }
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** Atribuído pelo adapter de persistência após o INSERT. */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new DomainException("ID do carrinho já foi atribuído");
        }
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public List<CartItem> getItems() {
        return List.copyOf(items);
    }
}
