package com.dd.backend.cart.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dd.backend.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

/** Regras de negócio do aggregate carrinho. */
class CartTest {

    @Test
    void deveSomarQuantidadeDoMesmoProduto() {
        Cart cart = Cart.openFor(1L);
        cart.addItem(5L, 1);
        cart.addItem(5L, 2);
        cart.addItem(7L, 1);

        assertEquals(2, cart.getItems().size());
        assertEquals(3, cart.getItems().stream()
                .filter(item -> item.getProductId() == 5L)
                .findFirst().orElseThrow().getQuantity());
    }

    @Test
    void deveRejeitarQuantidadeInvalida() {
        Cart cart = Cart.openFor(1L);
        assertThrows(DomainException.class, () -> cart.addItem(5L, 0));
    }

    @Test
    void removerItemInexistenteERejeitado() {
        Cart cart = Cart.openFor(1L);
        assertThrows(DomainException.class, () -> cart.removeItem(5L));
    }

    @Test
    void limparEsvaziaOCarrinho() {
        Cart cart = Cart.openFor(1L);
        cart.addItem(5L, 2);
        cart.clear();

        assertTrue(cart.isEmpty());
    }
}
