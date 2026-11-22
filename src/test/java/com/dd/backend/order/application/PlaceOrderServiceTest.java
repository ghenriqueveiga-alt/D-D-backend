package com.dd.backend.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dd.backend.cart.application.port.inbound.CartView;
import com.dd.backend.cart.application.port.inbound.ClearCartUseCase;
import com.dd.backend.cart.application.port.inbound.GetCartUseCase;
import com.dd.backend.catalog.application.port.inbound.AdjustStockUseCase;
import com.dd.backend.catalog.application.port.inbound.GetProductUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.customer.application.port.inbound.FindOrCreateCustomerUseCase;
import com.dd.backend.customer.application.port.inbound.GetCustomerUseCase;
import com.dd.backend.customer.domain.model.Customer;
import com.dd.backend.order.application.port.inbound.PlaceOrderCommand;
import com.dd.backend.order.domain.model.Order;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fluxo de registro de pedido (checkout) com fakes das portas — o estoque usa
 * o caso de uso real de movimentações sobre o domínio real de {@link Product}.
 */
class PlaceOrderServiceTest {

    private FakeOrderRepository orders;
    private FakeCustomerPort customers;
    private FakeProductCatalog products;
    private FakeCart cart;
    private FakeMovementRepository movements;
    private PlaceOrderService service;

    @BeforeEach
    void setUp() {
        orders = new FakeOrderRepository();
        customers = new FakeCustomerPort();
        customers.register(Customer.restore(
                10L, "Cliente Antigo", "antigo@exemplo.com", Instant.parse("2026-01-01T00:00:00Z")));
        products = new FakeProductCatalog();
        products.add(product(1L, "Teclado Mecânico", "200.00", 10));
        products.add(product(2L, "Mouse Gamer", "100.00", 2));
        cart = new FakeCart();
        movements = new FakeMovementRepository();
        // Caso de uso real de estoque (valida saldo e registra o histórico).
        var stockMovements = new StockMovementService(movements, new FakeAdjustStock(products));
        service = new PlaceOrderService(
                orders, customers, customers, products, cart, cart, stockMovements);
    }

    @Test
    void registraPedidoComEnderecoEPagamentoEBaixaEstoque() {
        PlaceOrderCommand command = new PlaceOrderCommand(
                10L, null, false,
                List.of(new PlaceOrderCommand.Item(1L, 2), new PlaceOrderCommand.Item(2L, 1)),
                address(), new BigDecimal("19.90"), PaymentMethod.PIX);

        Order order = service.place(command);

        assertNotNull(order.getId());
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(PaymentMethod.PIX, order.getPaymentMethod());
        // 2×200 + 1×100 + frete 19,90
        assertEquals(new BigDecimal("519.90"), order.getTotal().amount());
        assertEquals("01310-100", order.getAddress().cep());

        assertEquals(8, products.get(1L).getStock());
        assertEquals(1, products.get(2L).getStock());
        assertEquals(2, movements.count());
        assertTrue(movements.notes().contains("Pedido #" + order.getId()));
    }

    @Test
    void checkoutSemCadastroCriaClientePeloEmailEReutilizaNoSegundoPedido() {
        PlaceOrderCommand first = new PlaceOrderCommand(
                null, new PlaceOrderCommand.CustomerData("Maria Silva", "maria@exemplo.com"),
                false, List.of(new PlaceOrderCommand.Item(1L, 1)),
                address(), null, null);

        Order order = service.place(first);
        assertEquals(11L, order.getCustomerId());
        assertEquals(new BigDecimal("200.00"), order.getTotal().amount(), "frete nulo vira zero");
        assertEquals(OrderStatus.PENDING, order.getStatus());

        Order second = service.place(new PlaceOrderCommand(
                null, new PlaceOrderCommand.CustomerData("Maria Silva", "MARIA@exemplo.com"),
                false, List.of(new PlaceOrderCommand.Item(1L, 1)),
                address(), null, null));

        assertEquals(order.getCustomerId(), second.getCustomerId(), "mesmo e-mail reutiliza o cadastro");
        assertEquals(1, customers.createdCount(), "não cria cadastro duplicado");
    }

    @Test
    void recusaPedidoSemEstoqueSemGravarNada() {
        PlaceOrderCommand command = new PlaceOrderCommand(
                10L, null, false, List.of(new PlaceOrderCommand.Item(1L, 11)),
                address(), null, null);

        DomainException error = assertThrows(DomainException.class, () -> service.place(command));
        assertTrue(error.getMessage().contains("Estoque insuficiente"));

        assertEquals(0, orders.count(), "pedido não é gravado");
        assertEquals(0, movements.count(), "nenhuma movimentação é registrada");
        assertEquals(10, products.get(1L).getStock(), "saldo intacto");
    }

    @Test
    void itemDuplicadoSomaAsQuantidadesNaValidacaoDeSaldo() {
        PlaceOrderCommand command = new PlaceOrderCommand(
                10L, null, false,
                List.of(new PlaceOrderCommand.Item(1L, 6), new PlaceOrderCommand.Item(1L, 6)),
                address(), null, null);

        assertThrows(DomainException.class, () -> service.place(command));

        assertEquals(0, orders.count());
        assertEquals(10, products.get(1L).getStock());
    }

    @Test
    void pedidoDoCarrinhoUsaSnapshotDoCarrinhoEEsvazia() {
        cart.put(10L, new CartView.CartItemView(1L, "Teclado Mecânico", Money.of("190.00"), 3, Money.of("570.00")));

        PlaceOrderCommand command = new PlaceOrderCommand(
                10L, null, true, null, address(), null, null);

        Order order = service.place(command);

        assertEquals(new BigDecimal("570.00"), order.getTotal().amount(), "preço vem do snapshot do carrinho");
        assertEquals("Teclado Mecânico", order.getItems().get(0).getProductTitle());
        assertTrue(cart.isCleared(), "carrinho é esvaziado após a compra");
        assertEquals(7, products.get(1L).getStock());
        assertEquals(1, movements.count());
    }

    @Test
    void identificacaoDoCompradorEAObrigatoria() {
        PlaceOrderCommand semNada = new PlaceOrderCommand(
                null, null, false, List.of(new PlaceOrderCommand.Item(1L, 1)),
                address(), null, null);

        DomainException error = assertThrows(DomainException.class, () -> service.place(semNada));
        assertTrue(error.getMessage().contains("customerId"));
        assertEquals(0, orders.count());
    }

    @Test
    void clienteIdInexistenteERecusado() {
        PlaceOrderCommand command = new PlaceOrderCommand(
                999L, null, false, List.of(new PlaceOrderCommand.Item(1L, 1)),
                address(), null, null);

        assertThrows(ResourceNotFoundException.class, () -> service.place(command));
        assertEquals(0, orders.count());
    }

    @Test
    void enderecoObrigatorioERecusadoAntesDeQualquerEscrita() {
        PlaceOrderCommand command = new PlaceOrderCommand(
                10L, null, false, List.of(new PlaceOrderCommand.Item(1L, 1)),
                null, null, null);

        DomainException error = assertThrows(DomainException.class, () -> service.place(command));
        assertTrue(error.getMessage().contains("Endereço de entrega"));
        assertEquals(0, orders.count());
        assertEquals(0, movements.count());
    }

    @Test
    void carrinhoVazioDoFluxoDeCarrinhoERecusado() {
        PlaceOrderCommand command = new PlaceOrderCommand(
                10L, null, true, null, address(), null, null);

        DomainException error = assertThrows(DomainException.class, () -> service.place(command));
        assertTrue(error.getMessage().contains("Carrinho vazio"));
        assertEquals(0, orders.count());
    }

    private static ShippingAddress address() {
        return ShippingAddress.of(
                "Maria da Silva", "11 99999-9999", "01310-100",
                "Avenida Paulista", "1000", "apto 42", "São Paulo", "SP");
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

        int count() {
            return stored.size();
        }
    }

    /** Fake das portas de cliente (busca por id e get-or-create por e-mail). */
    private static final class FakeCustomerPort implements GetCustomerUseCase, FindOrCreateCustomerUseCase {

        private final Map<Long, Customer> byId = new LinkedHashMap<>();
        private final Map<String, Customer> byEmail = new HashMap<>();
        private long nextId = 11;
        private int created = 0;

        void register(Customer customer) {
            byId.put(customer.getId(), customer);
            byEmail.put(customer.getEmail(), customer);
        }

        @Override
        public Customer execute(Long customerId) {
            Customer customer = byId.get(customerId);
            if (customer == null) {
                throw ResourceNotFoundException.of("Cliente", customerId);
            }
            return customer;
        }

        @Override
        public Customer findOrCreate(String name, String email) {
            Customer existing = byEmail.get(email.trim().toLowerCase());
            if (existing != null) {
                return existing;
            }
            Customer registered = Customer.register(name, email);
            registered.assignId(nextId++);
            register(registered);
            created++;
            return registered;
        }

        int createdCount() {
            return created;
        }
    }

    /** Fake da porta de busca do catálogo (domínio real de produto). */
    private static final class FakeProductCatalog implements GetProductUseCase {

        private final Map<Long, Product> products = new LinkedHashMap<>();

        void add(Product product) {
            products.put(product.getId(), product);
        }

        Product get(Long id) {
            return products.get(id);
        }

        @Override
        public Product execute(Long productId) {
            Product product = products.get(productId);
            if (product == null) {
                throw ResourceNotFoundException.of("Produto", productId);
            }
            return product;
        }
    }

    /** Fake das portas de carrinho (consulta e limpeza). */
    private static final class FakeCart implements GetCartUseCase, ClearCartUseCase {

        private final Map<Long, CartView> carts = new LinkedHashMap<>();
        private boolean cleared;

        void put(Long customerId, CartView.CartItemView item) {
            carts.put(customerId, new CartView(customerId, List.of(item), item.subtotal()));
        }

        @Override
        public CartView get(Long customerId) {
            return carts.getOrDefault(customerId, new CartView(customerId, List.of(), Money.zero()));
        }

        @Override
        public void clear(Long customerId) {
            cleared = true;
            carts.put(customerId, new CartView(customerId, List.of(), Money.zero()));
        }

        boolean isCleared() {
            return cleared;
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

        List<String> notes() {
            return stored.stream().map(StockMovement::getNote).toList();
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
