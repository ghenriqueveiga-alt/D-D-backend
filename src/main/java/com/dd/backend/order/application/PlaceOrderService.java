package com.dd.backend.order.application;

import com.dd.backend.cart.application.port.inbound.CartView;
import com.dd.backend.cart.application.port.inbound.ClearCartUseCase;
import com.dd.backend.cart.application.port.inbound.GetCartUseCase;
import com.dd.backend.catalog.application.port.inbound.GetProductUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.customer.application.port.inbound.FindOrCreateCustomerUseCase;
import com.dd.backend.customer.application.port.inbound.GetCustomerUseCase;
import com.dd.backend.customer.domain.model.Customer;
import com.dd.backend.order.application.port.inbound.PlaceOrderCommand;
import com.dd.backend.order.application.port.inbound.PlaceOrderUseCase;
import com.dd.backend.order.domain.model.Order;
import com.dd.backend.order.domain.model.OrderItem;
import com.dd.backend.order.domain.repository.OrderRepository;
import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.Money;
import com.dd.backend.stock.application.port.inbound.RegisterStockMovementUseCase;
import com.dd.backend.stock.domain.model.StockMovement;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de venda (checkout): resolve o comprador, valida os itens no
 * catálogo e o saldo em estoque, monta o aggregate {@link Order} com endereço
 * e frete, baixa o estoque registrando a movimentação de saída e, no fluxo de
 * carrinho, esvazia o carrinho após a compra.
 *
 * <p>Tudo roda em uma única transação: qualquer recusa (estoque, domínio)
 * desfaz o pedido já gravado.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final GetCustomerUseCase getCustomer;
    private final FindOrCreateCustomerUseCase findOrCreateCustomer;
    private final GetProductUseCase getProduct;
    private final GetCartUseCase getCart;
    private final ClearCartUseCase clearCart;
    private final RegisterStockMovementUseCase registerStockMovement;

    @Override
    public Order place(PlaceOrderCommand command) {
        if (command == null) {
            throw new DomainException("Comando do pedido é obrigatório");
        }
        Long customerId = resolveCustomerId(command);

        List<OrderItem> items = command.fromCart()
                ? itemsFromCart(command, customerId)
                : itemsFromRequest(command);

        // Valida o saldo de TODOS os itens antes de qualquer escrita.
        Map<Long, Integer> demanded = mergeByProduct(items);
        validateStock(demanded);

        Order order = Order.place(customerId, items, command.address(), toMoney(command.shipping()));
        if (command.paymentMethod() != null) {
            order.pay(command.paymentMethod());
        }
        order = orderRepository.save(order);

        releaseStock(demanded, order.getId());
        if (command.fromCart()) {
            clearCart.clear(customerId);
        }
        return order;
    }

    /** Cliente informado por id (validado) ou criado a partir do checkout. */
    private Long resolveCustomerId(PlaceOrderCommand command) {
        if (command.customerId() != null) {
            getCustomer.execute(command.customerId());
            return command.customerId();
        }
        PlaceOrderCommand.CustomerData customer = command.customer();
        if (customer == null) {
            throw new DomainException("Informe customerId ou os dados do cliente (nome e e-mail)");
        }
        Customer registered = findOrCreateCustomer.findOrCreate(customer.name(), customer.email());
        return registered.getId();
    }

    private List<OrderItem> itemsFromCart(PlaceOrderCommand command, Long customerId) {
        CartView cart = getCart.get(customerId);
        if (cart.items().isEmpty()) {
            throw new DomainException("Carrinho vazio para o cliente " + customerId);
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

    /** Soma quantidades por produto (itens duplicados no pedido). */
    private Map<Long, Integer> mergeByProduct(List<OrderItem> items) {
        Map<Long, Integer> demanded = new LinkedHashMap<>();
        items.forEach(item ->
                demanded.merge(item.getProductId(), item.getQuantity(), Integer::sum));
        return demanded;
    }

    /** Recusa o pedido inteiro quando algum item não tem saldo disponível. */
    private void validateStock(Map<Long, Integer> demanded) {
        demanded.forEach((productId, quantity) -> {
            Product product = getProduct.execute(productId);
            if (product.getStock() < quantity) {
                throw new DomainException(
                        "Estoque insuficiente para \"" + product.getTitle() + "\": disponível "
                                + product.getStock() + ", pedido " + quantity);
            }
        });
    }

    /** Baixa o saldo e registra a saída no histórico, com o nº do pedido. */
    private void releaseStock(Map<Long, Integer> demanded, Long orderId) {
        demanded.forEach((productId, quantity) ->
                registerStockMovement.execute(new RegisterStockMovementUseCase.Command(
                        productId, StockMovement.Type.OUT, quantity, "Pedido #" + orderId)));
    }

    private Money toMoney(BigDecimal shipping) {
        return shipping == null ? Money.zero() : Money.of(shipping);
    }
}
