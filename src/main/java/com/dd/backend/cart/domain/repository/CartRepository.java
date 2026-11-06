package com.dd.backend.cart.domain.repository;

import com.dd.backend.cart.domain.model.Cart;
import java.util.Optional;

/** Porta de saída (hexagonal) de persistência do carrinho. */
public interface CartRepository {

    Cart save(Cart cart);

    Optional<Cart> findByCustomerId(Long customerId);
}
