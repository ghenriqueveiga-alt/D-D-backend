package com.dd.backend.order.application;

import com.dd.backend.order.application.port.inbound.OrderLifecycleUseCase;
import com.dd.backend.order.domain.model.Order;
import com.dd.backend.order.domain.model.PaymentMethod;
import com.dd.backend.order.domain.repository.OrderRepository;
import com.dd.backend.shared.domain.ResourceNotFoundException;
import com.dd.backend.stock.application.port.inbound.RegisterStockMovementUseCase;
import com.dd.backend.stock.domain.model.StockMovement;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transições de estado do pedido — toda a regra fica no aggregate {@link Order}.
 *
 * <p>O cancelamento devolve ao estoque as unidades baixadas na compra e
 * registra a entrada no histórico (somente pedidos do fluxo de checkout, que
 * são os únicos com endereço e que baixaram saldo).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderLifecycleService implements OrderLifecycleUseCase {

    private final OrderRepository orderRepository;
    private final RegisterStockMovementUseCase registerStockMovement;

    @Override
    public Order pay(Long orderId, PaymentMethod method) {
        Order order = load(orderId);
        order.pay(method);
        return orderRepository.save(order);
    }

    @Override
    public Order ship(Long orderId) {
        Order order = load(orderId);
        order.ship();
        return orderRepository.save(order);
    }

    @Override
    public Order deliver(Long orderId) {
        Order order = load(orderId);
        order.deliver();
        return orderRepository.save(order);
    }

    @Override
    public Order cancel(Long orderId) {
        Order order = load(orderId);
        order.cancel();
        order = orderRepository.save(order);
        restoreStock(order);
        return order;
    }

    /** Devolve as unidades ao saldo e registra a entrada no histórico. */
    private void restoreStock(Order order) {
        if (order.getAddress() == null) {
            // Pedido anterior ao checkout: não baixou estoque, não devolve.
            return;
        }
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        order.getItems().forEach(item ->
                quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum));
        quantities.forEach((productId, quantity) ->
                registerStockMovement.execute(new RegisterStockMovementUseCase.Command(
                        productId, StockMovement.Type.IN, quantity,
                        "Pedido #" + order.getId() + " cancelado")));
    }

    private Order load(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Pedido", orderId));
    }
}
