package com.dd.backend.order.application;

import com.dd.backend.cart.application.port.inbound.CartView;
import com.dd.backend.cart.application.port.inbound.ClearCartUseCase;
import com.dd.backend.cart.application.port.inbound.GetCartUseCase;
import com.dd.backend.catalog.application.port.inbound.GetProductUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.customer.application.port.inbound.GetCustomerUseCase;
import com.dd.backend.order.application.port.inbound.PlaceOrderCommand;
import com.dd.backend.order.application.port.inbound.PlaceOrderUseCase;
import com.dd.backend.order.domain.model.Order;
import com.dd.backend.order.domain.model.OrderItem;
import com.dd.backend.order.domain.repository.OrderRepository;
import com.dd.backend.shared.domain.DomainException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Caso de uso de venda: valida cliente e itens (catálogo), monta o aggregate
 * {@link Order} e, no fluxo de carrinho, esvazia o carrinho após a compra.
 */
@Service
@RequiredArgsConstructor
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final GetCustomerUseCase getCustomer;
    private final GetProductUseCase getProduct;
    private final GetCartUseCase getCart;
    private final ClearCartUseCase clearCart;

    @Override
    public Order place(PlaceOrderCommand command) {
        if (command == null || command.customerId() == null) {
            throw new DomainException("customerId é obrigatório");
        }
        getCustomer.execute(command.customerId());

        List<OrderItem> items = command.fromCart()
                ? itemsFromCart(command)
                : itemsFromRequest(command);

        Order order = orderRepository.save(Order.place(command.customerId(), items));
        if (command.fromCart()) {
            clearCart.clear(command.customerId());
        }
        return order;
    }

    private List<OrderItem> itemsFromCart(PlaceOrderCommand command) {
        CartView cart = getCart.get(command.customerId());
        if (cart.items().isEmpty()) {
            throw new DomainException("Carrinho vazio para o cliente " + command.customerId());
        }
        return cart.items().stream()
                .map(item -> OrderItem.of(item.productId(), item.title(), item.unitPrice(), item.quantity()))
                .toList();
    }

    private List<OrderItem> itemsFromRequest(PlaceOrderCommand command) {
        if (command.items() == null || command.items().isEmpty()) {
            throw new DomainException("Informe os itens do pedido ou use fromCart=true");
        }
        return command.items().stream()
                .map(this::toOrderItem)
                .toList();
    }

    private OrderItem toOrderItem(PlaceOrderCommand.Item item) {
        if (item.productId() == null) {
            throw new DomainException("productId é obrigatório em cada item");
        }
        if (item.quantity() == null) {
            throw new DomainException("quantity é obrigatória em cada item");
        }
        Product product = getProduct.execute(item.productId());
        return OrderItem.of(product.getId(), product.getTitle(), product.getPrice(), item.quantity());
    }
}
