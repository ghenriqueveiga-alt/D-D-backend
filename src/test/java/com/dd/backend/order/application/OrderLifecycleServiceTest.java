package com.dd.backend.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dd.backend.catalog.application.port.inbound.AdjustStockUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.order.domain.model.Order;
import com.dd.backend.order.domain.model.OrderItem;
import com.dd.backend.order.domain.model.OrderStatus;
import com.dd.backend.order.domain.model.PaymentMethod;
import com.dd.backend.order.domain.model.ShippingAddress;
import com.dd.backend.order.domain.repository.OrderRepository;
import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.Money;
import com.dd.backend.shared.domain.ResourceNotFoundException;
import com.dd.backend.stock.application.StockMovementService;
import com.dd.backend.stock.domain.model.StockMovement;
import com.dd.backend.stock.domain.repository.StockMovementRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Ciclo de vida do pedido com fakes das portas — o cancelamento devolve o
 * estoque pelo caso de uso real de movimentações.
 */
class OrderLifecycleServiceTest {

    private FakeOrderRepository orders;
    private FakeProductCatalog products;
    private FakeMovementRepository movements;
    private OrderLifecycleService service;

    @BeforeEach
    void setUp() {
        orders = new FakeOrderRepository();
        products = new FakeProductCatalog();
        products.add(product(1L, "Teclado Mecânico", "200.00", 7));
        movements = new FakeMovementRepository();
        var stockMovements = new StockMovementService(movements, new FakeAdjustStock(products));
        service = new OrderLifecycleService(orders, stockMovements);
    }

    @Test
    void cancelarPedidoDoCheckoutDevolveEstoqueERegistraEntrada() {
        Order order = checkoutOrder(3);
        assertEquals(4, products.get(1L).getStock(), "baixa feita na compra");

        Order cancelled = service.cancel(order.getId());

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(7, products.get(1L).getStock(), "unidades voltam ao saldo");
        assertEquals(1, movements.count());
        assertEquals(StockMovement.Type.IN, movements.findAll().get(0).getType());
        assertTrue(movements.findAll().get(0).getNote().contains("cancelado"));
    }

    @Test
    void cancelarPedidoLegadoSemEnderecoNaoDevolveEstoque() {
        // Pedido anterior ao checkout: nunca baixou saldo, não pode devolver.
        Order legacy = orders.save(Order.restore(
                50L, 10L, List.of(item(1L, 3)), OrderStatus.PAID, PaymentMethod.PIX,
                Instant.parse("2026-01-01T00:00:00Z"), null, null, null));

        Order cancelled = service.cancel(legacy.getId());

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(7, products.get(1L).getStock(), "saldo do legado fica como estava");
        assertEquals(0, movements.count());
    }

    @Test
    void cancelarPedidoEntregueERecusadoSemTocarNoEstoque() {
        Order order = checkoutOrder(3);
        order.pay(PaymentMethod.PIX);
        order.ship();
        order.deliver();

        assertThrows(DomainException.class, () -> service.cancel(order.getId()));

        assertEquals(OrderStatus.DELIVERED, orders.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(4, products.get(1L).getStock(), "saldo intacto");
        assertEquals(0, movements.count());
    }

    @Test
    void pagarPedidoPendenteMudaParaPAIDEInvalidaTransicao() {
        Order order = checkoutOrder(1);

        Order paid = service.pay(order.getId(), PaymentMethod.BOLETO);
        assertEquals(OrderStatus.PAID, paid.getStatus());
        assertEquals(PaymentMethod.BOLETO, paid.getPaymentMethod());

        assertThrows(DomainException.class, () -> service.pay(order.getId(), PaymentMethod.PIX));
    }

    @Test
    void pedidoInexistenteLancaNaoEncontrado() {
        assertThrows(ResourceNotFoundException.class, () -> service.pay(999L, PaymentMethod.PIX));
        assertThrows(ResourceNotFoundException.class, () -> service.cancel(999L));
    }

    /** Grava um pedido do fluxo de checkout com 1 item e o estoque já baixado. */
    private Order checkoutOrder(int quantity) {
        products.get(1L).adjustStock(-quantity);
        return orders.save(Order.place(
                10L,
                List.of(item(1L, quantity)),
                ShippingAddress.of(
                        "Maria da Silva", "11 99999-9999", "01310-100",
                        "Avenida Paulista", "1000", null, "São Paulo", "SP"),
                Money.zero()));
    }

    private static OrderItem item(long productId, int quantity) {
        return OrderItem.of(productId, "Teclado Mecânico", Money.of("200.00"), quantity);
    }

    private static Product product(long id, String title, String price, int stock) {
        return Product.restore(id, "SKU-" + id, title, null, Money.of(price),
                "geral", "Marca", null, stock, Instant.parse("2026-01-01T00:00:00Z"));
    }

    /** Fake em memória da porta de pedidos. */
    private static final class FakeOrderRepository implements OrderRepository {

        private final Map<Long, Order> stored = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Order save(Order order) {
            if (order.getId() == null) {
                order.assignId(nextId++);
            }
            stored.put(order.getId(), order);
            return order;
        }

        @Override
        public Optional<Order> findById(Long id) {
            return Optional.ofNullable(stored.get(id));
        }

        @Override
        public List<Order> findAll() {
            return List.copyOf(stored.values());
        }

        @Override
        public List<Order> findByCustomerId(Long customerId) {
            return stored.values().stream().filter(o -> o.getCustomerId().equals(customerId)).toList();
        }
    }

    /** Fake do catálogo: guarda os produtos reais usados pelo ajuste. */
    private static final class FakeProductCatalog {

        private final Map<Long, Product> products = new LinkedHashMap<>();

        void add(Product product) {
            products.put(product.getId(), product);
        }

        Product get(Long id) {
            return products.get(id);
        }
    }

    /** Fake em memória do histórico de movimentações. */
    private static final class FakeMovementRepository implements StockMovementRepository {

        private final List<StockMovement> stored = new ArrayList<>();
        private long nextId = 1;

        @Override
        public StockMovement save(StockMovement movement) {
            if (movement.getId() == null) {
                movement.assignId(nextId++);
            }
            stored.add(movement);
            return movement;
        }

        @Override
        public List<StockMovement> findAll() {
            return List.copyOf(stored);
        }

        @Override
        public List<StockMovement> findByProductId(Long productId) {
            return stored.stream().filter(m -> m.getProductId().equals(productId)).toList();
        }

        @Override
        public long count() {
            return stored.size();
        }
    }

    /** Fake da porta de ajuste de estoque (regras reais do domínio). */
    private static final class FakeAdjustStock implements AdjustStockUseCase {

        private final FakeProductCatalog products;

        FakeAdjustStock(FakeProductCatalog products) {
            this.products = products;
        }

        @Override
        public Product execute(Long productId, int delta) {
            Product product = products.get(productId);
            if (product == null) {
                throw ResourceNotFoundException.of("Produto", productId);
            }
            product.adjustStock(delta);
            return product;
        }
    }
}
