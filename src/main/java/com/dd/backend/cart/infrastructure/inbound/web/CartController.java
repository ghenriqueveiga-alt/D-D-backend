package com.dd.backend.cart.infrastructure.inbound.web;

import com.dd.backend.cart.application.port.inbound.AddCartItemUseCase;
import com.dd.backend.cart.application.port.inbound.ClearCartUseCase;
import com.dd.backend.cart.application.port.inbound.GetCartUseCase;
import com.dd.backend.cart.application.port.inbound.RemoveCartItemUseCase;
import com.dd.backend.cart.infrastructure.inbound.web.dto.AddCartItemRequest;
import com.dd.backend.cart.infrastructure.inbound.web.dto.CartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adapter de entrada: carrinho de compras. */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Carrinho", description = "Carrinho de compras persistido por cliente")
public class CartController {

    private final AddCartItemUseCase addCartItem;
    private final RemoveCartItemUseCase removeCartItem;
    private final ClearCartUseCase clearCart;
    private final GetCartUseCase getCart;

    @GetMapping("/{customerId}")
    @Operation(summary = "Consulta o carrinho do cliente (com preços do catálogo)")
    public CartResponse get(@PathVariable Long customerId) {
        return CartResponse.from(getCart.get(customerId));
    }

    @PostMapping("/{customerId}/items")
    @Operation(summary = "Adiciona um item ao carrinho (soma a quantidade se repetido)")
    public CartResponse add(@PathVariable Long customerId,
                            @Valid @RequestBody AddCartItemRequest request) {
        return CartResponse.from(addCartItem.add(customerId, request.productId(), request.quantity()));
    }

    @DeleteMapping("/{customerId}/items/{productId}")
    @Operation(summary = "Remove um item do carrinho")
    public CartResponse remove(@PathVariable Long customerId, @PathVariable Long productId) {
        return CartResponse.from(removeCartItem.remove(customerId, productId));
    }

    @DeleteMapping("/{customerId}")
    @Operation(summary = "Esvazia o carrinho do cliente")
    public ResponseEntity<Void> clear(@PathVariable Long customerId) {
        clearCart.clear(customerId);
        return ResponseEntity.noContent().build();
    }
}
