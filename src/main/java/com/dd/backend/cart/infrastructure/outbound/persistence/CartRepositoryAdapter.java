package com.dd.backend.cart.infrastructure.outbound.persistence;

import com.dd.backend.cart.domain.model.Cart;
import com.dd.backend.cart.domain.model.CartItem;
import com.dd.backend.cart.domain.repository.CartRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CartRepositoryAdapter implements CartRepository {

    private final CartJpaRepository jpaRepository;

    @Override
    public Cart save(Cart cart) {
        CartJpaEntity entity = cart.getId() == null
                ? new CartJpaEntity()
                : jpaRepository.findById(cart.getId()).orElseThrow();
        entity.setCustomerId(cart.getCustomerId());
        syncItems(entity, cart.getItems());
        CartJpaEntity saved = jpaRepository.save(entity);
        if (cart.getId() == null) {
            cart.assignId(saved.getId());
        }
        return cart;
    }

    @Override
    public Optional<Cart> findByCustomerId(Long customerId) {
        return jpaRepository.findByCustomerId(customerId).map(this::toDomain);
    }

    /**
     * Sincroniza a coleção gerenciada pelo Hibernate: quantidades mudam no lugar,
     * itens novos são inseridos e itens removidos saem por orphanRemoval. Nunca
     * re-inserimos itens já existentes — isso violaria a unique (cart_id, product_id).
     */
    private void syncItems(CartJpaEntity entity, List<CartItem> items) {
        entity.getItems().removeIf(existing -> items.stream()
                .noneMatch(item -> item.getProductId().equals(existing.getProductId())));
        for (CartItem item : items) {
            entity.getItems().stream()
                    .filter(existing -> existing.getProductId().equals(item.getProductId()))
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> existing.setQuantity(item.getQuantity()),
                            () -> {
                                CartItemJpaEntity created = new CartItemJpaEntity();
                                created.setProductId(item.getProductId());
                                created.setQuantity(item.getQuantity());
                                entity.getItems().add(created);
                            });
        }
    }

    private Cart toDomain(CartJpaEntity entity) {
        List<CartItem> items = entity.getItems().stream()
                .map(item -> CartItem.of(item.getProductId(), item.getQuantity()))
                .toList();
        return Cart.restore(entity.getId(), entity.getCustomerId(), items);
    }
}
