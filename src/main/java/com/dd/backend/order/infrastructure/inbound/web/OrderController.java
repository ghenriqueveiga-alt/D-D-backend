package com.dd.backend.order.infrastructure.inbound.web;

import com.dd.backend.order.application.port.inbound.OrderLifecycleUseCase;
import com.dd.backend.order.application.port.inbound.OrderQueryUseCase;
import com.dd.backend.order.application.port.inbound.PlaceOrderUseCase;
import com.dd.backend.order.domain.model.Order;
import com.dd.backend.order.infrastructure.inbound.web.dto.OrderResponse;
import com.dd.backend.order.infrastructure.inbound.web.dto.PayOrderRequest;
import com.dd.backend.order.infrastructure.inbound.web.dto.PlaceOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Adapter de entrada: pedidos de venda. */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Vendas: registro, consulta e ciclo de vida")
public class OrderController {

    private final PlaceOrderUseCase placeOrder;
    private final OrderQueryUseCase queryOrders;
    private final OrderLifecycleUseCase lifecycle;

    @PostMapping
    @Operation(summary = "Registra um pedido (itens explícitos ou a partir do carrinho)")
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        Order order = placeOrder.place(request.toCommand());
        return ResponseEntity
                .created(URI.create("/api/orders/" + order.getId()))
                .body(OrderResponse.from(order));
    }

    @GetMapping
    @Operation(summary = "Lista pedidos (filtro opcional por cliente)")
    public List<OrderResponse> list(@RequestParam(name = "customerId", required = false) Long customerId) {
        List<Order> orders = customerId == null
                ? queryOrders.list()
                : queryOrders.listByCustomer(customerId);
        return orders.stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um pedido pelo id")
    public OrderResponse get(@PathVariable Long id) {
        return OrderResponse.from(queryOrders.get(id));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "PAGA o pedido: PENDING → PAID")
    public OrderResponse pay(@PathVariable Long id, @Valid @RequestBody PayOrderRequest request) {
        return OrderResponse.from(lifecycle.pay(id, request.paymentMethod()));
    }

    @PostMapping("/{id}/ship")
    @Operation(summary = "Envia o pedido: PAID → SHIPPED")
    public OrderResponse ship(@PathVariable Long id) {
        return OrderResponse.from(lifecycle.ship(id));
    }

    @PostMapping("/{id}/deliver")
    @Operation(summary = "Entrega o pedido: SHIPPED → DELIVERED")
    public OrderResponse deliver(@PathVariable Long id) {
        return OrderResponse.from(lifecycle.deliver(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela o pedido: PENDING/PAID → CANCELLED")
    public OrderResponse cancel(@PathVariable Long id) {
        return OrderResponse.from(lifecycle.cancel(id));
    }
}
