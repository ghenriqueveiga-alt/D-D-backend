package com.dd.backend.cart.application;

import com.dd.backend.cart.application.port.inbound.AddCartItemUseCase;
import com.dd.backend.cart.application.port.inbound.CartView;
import com.dd.backend.cart.application.port.inbound.ClearCartUseCase;
import com.dd.backend.cart.application.port.inbound.GetCartUseCase;
import com.dd.backend.cart.application.port.inbound.RemoveCartItemUseCase;
import com.dd.backend.cart.domain.model.Cart;
import com.dd.backend.cart.domain.repository.CartRepository;
import com.dd.backend.catalog.application.port.inbound.GetProductUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.customer.application.port.inbound.GetCustomerUseCase;
import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.Money;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Casos de uso do carrinho.
 *
 * <p>Colabora com outros contextos apenas através das ports públicas:
 * {@link GetCustomerUseCase} (valida o cliente) e {@link GetProductUseCase}
 * (valida o produto e enriquece a resposta com título/preço).</p>
 */
@Service
@RequiredArgsConstructor
public class CartService implements AddCartItemUseCase, RemoveCartItemUseCase, ClearCartUseCase, GetCartUseCase {

    private final CartRepository cartRepository;
    private final GetCustomerUseCase getCustomer;
    private final GetProductUseCase getProduct;

    @Override
    public CartView add(Long customerId, Long productId, int quantity) {
        getCustomer.execute(customerId);
        getProduct.execute(productId);
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> Cart.openFor(customerId));
        cart.addItem(productId, quantity);
        return toView(cartRepository.save(cart));
    }

    @Override
    public CartView remove(Long customerId, Long productId) {
        getCustomer.execute(customerId);
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new DomainException(
                        "Carrinho inexistente para o cliente " + customerId));
        cart.removeItem(productId);
        return toView(cartRepository.save(cart));
    }

    @Override
    public void clear(Long customerId) {
        getCustomer.execute(customerId);
        cartRepository.findByCustomerId(customerId).ifPresent(cart -> {
            cart.clear();
            cartRepository.save(cart);
        });
    }

    @Override
    public CartView get(Long customerId) {
        getCustomer.execute(customerId);
        return cartRepository.findByCustomerId(customerId)
                .map(this::toView)
                .orElseGet(() -> new CartView(customerId, List.of(), Money.zero()));
    }

    /** Monta a visão do carrinho resolvendo título e preço no catálogo. */
    private CartView toView(Cart cart) {
        List<CartView.CartItemView> items = cart.getItems().stream()
                .map(item -> {
                    Product product = getProduct.execute(item.getProductId());
                    return new CartView.CartItemView(
                            product.getId(),
                            product.getTitle(),
                            product.getPrice(),
                            item.getQuantity(),
                            product.getPrice().times(item.getQuantity()));
                })
                .toList();
        Money total = items.stream()
                .map(CartView.CartItemView::subtotal)
                .reduce(Money.zero(), Money::plus);
        return new CartView(cart.getCustomerId(), items, total);
    }
}
