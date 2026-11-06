# D-D Backend

API de vendas de itens reais buscados na internet ([DummyJSON](https://dummyjson.com)), construída com **DDD + arquitetura hexagonal** (ports & adapters).

## Stack

| Item | Tecnologia |
|---|---|
| Framework | Spring Boot **4.1.2** |
| Linguagem | Java **21** (toolchain) |
| Build | Gradle 9 (Kotlin DSL) |
| Banco | PostgreSQL |
| Migrações | Liquibase |
| Segurança | Spring Security + JWT (jjwt 0.13.0) |
| Documentação | SpringDoc OpenAPI (Swagger UI) |
| ORM | Spring Data JPA + Lombok |
| HTTP client | `spring-boot-starter-restclient` (Spring Framework 7) |

## Arquitetura (DDD + hexagonal)

Cada contexto de negócio é um pacote autocontido sob `com.dd.backend.{context}`, com separação estrita entre **domínio**, **casos de uso** (ports) e **adapters**:

```
src/main/java/com/dd/backend/
├── shared/                          # kernel compartilhado
│   ├── domain/                      # Money, DomainException, ResourceNotFoundException,
│   │                                # ExternalServiceException
│   └── infrastructure/web/          # RestExceptionHandler (ProblemDetail RFC 7807)
├── catalog/                         # contexto: catálogo de produtos
│   ├── domain/model/Product         # aggregate (externalId = identidade na fonte)
│   ├── domain/repository/           # porta de saída ProductRepository
│   ├── application/port/inbound/    # SyncCatalogUseCase, ListProducts, GetProduct
│   ├── application/port/outbound/   # ProductSource (porta para a fonte externa)
│   ├── application/                 # CatalogSyncService (upsert), CatalogQueryService
│   └── infrastructure/
│       ├── inbound/web/             # ProductController, CatalogController + DTOs
│       └── outbound/
│           ├── persistence/         # adapter JPA (entity + repository + adapter)
│           └── external/            # DummyJsonProductSource (adapter da fonte)
├── customer/                        # contexto: clientes
├── cart/                            # contexto: carrinho por cliente
└── order/                           # contexto: pedidos (aggregate Order)
    └── domain/model/Order           # ciclo de vida PENDING→PAID→SHIPPED→DELIVERED
                                     # (e cancel em PENDING/PAID)
```

**Fluxo de uma requisição (ports & adapters):**

```
HTTP → Controller (adapter inbound) → UseCase (porta inbound) → Service (caso de uso)
     → Domain (regras de negócio) ←→ Repository/Source (porta outbound) ←→ Adapter (JPA/HTTP)
```

Regras importantes:

- O **domínio não conhece** Spring/JPA/HTTP — só o kernel `shared.domain` e as próprias ports.
- Contextos colaboram **apenas por ports públicas** (ex.: `cart` usa `GetProductUseCase` e `GetCustomerUseCase`).
- A fonte externa de itens é uma **porta de saída** (`ProductSource`): trocar DummyJSON por outra API = novo adapter, zero mudança no domínio.
- Coleções JPA lazy exigem `@Transactional` nos adapters de persistência (o mapeamento para o domínio acontece dentro da transação).

## Endpoints

Base: `http://localhost:8080` · Documentação: `/swagger-ui.html` · OpenAPI: `/v3/api-docs`

### Catálogo (itens vindos da internet)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/catalog/sync` | Busca **todos** os itens no DummyJSON e faz upsert no catálogo local |
| `GET` | `/api/products` | Lista produtos (filtro opcional `?category=smartphones`) |
| `GET` | `/api/products/{id}` | Detalha um produto |

### Clientes

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/customers` | Cadastra cliente (`name`, `email` — 409 se o e-mail existir) |
| `GET` | `/api/customers` | Lista clientes |
| `GET` | `/api/customers/{id}` | Detalha cliente |

### Carrinho (1 por cliente)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/cart/{customerId}` | Consulta com preços/títulos do catálogo |
| `POST` | `/api/cart/{customerId}/items` | Adiciona item (`productId`, `quantity` — soma se repetido) |
| `DELETE` | `/api/cart/{customerId}/items/{productId}` | Remove item |
| `DELETE` | `/api/cart/{customerId}` | Esvazia o carrinho |

### Pedidos

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/orders` | Registra pedido: `{"customerId":1,"fromCart":true}` **ou** `{"customerId":1,"items":[{"productId":10,"quantity":3}]}` — no fluxo de carrinho, esvazia o carrinho após a compra |
| `GET` | `/api/orders` | Lista pedidos (filtro opcional `?customerId=1`) |
| `GET` | `/api/orders/{id}` | Detalha pedido |
| `POST` | `/api/orders/{id}/pay` | `PENDING → PAID` (`{"paymentMethod":"PIX"}` — PIX, CREDIT_CARD, DEBIT_CARD, BOLETO) |
| `POST` | `/api/orders/{id}/ship` | `PAID → SHIPPED` |
| `POST` | `/api/orders/{id}/deliver` | `SHIPPED → DELIVERED` |
| `POST` | `/api/orders/{id}/cancel` | `PENDING/PAID → CANCELLED` |

### Códigos de erro (ProblemDetail RFC 7807)

| HTTP | Quando |
|---|---|
| `400` | JSON inválido ou validação de campos (`errors` no corpo) |
| `404` | Recurso inexistente (produto/cliente/pedido) |
| `409` | Violação de restrição única (e-mail duplicado) |
| `422` | Regra de negócio violada (transição de status inválida, carrinho vazio…) |
| `502` | Falha ao consultar a fonte externa (DummyJSON) |

## Como rodar

### 1. Banco (PostgreSQL)

```bash
docker run -d --name dd-postgres \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=dd_store -p 5432:5432 postgres:16-alpine
```

As migrations do Liquibase (`src/main/resources/db/changelog/changes/`) executam automaticamente na subida.

### 2. Aplicação

```bash
./gradlew bootRun          # ou:
./gradlew bootJar
java -jar build/libs/d-d-backend-0.0.1-SNAPSHOT.jar
```

### 3. Popular o catálogo com os itens reais

```bash
curl -X POST http://localhost:8080/api/catalog/sync
# {"productsSynchronized":194}
```

### Variáveis de ambiente (opcionais)

| Variável | Padrão |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/dd_store` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `SERVER_PORT` | `8080` |
| `SHOW_SQL` | `false` |

### Testes

```bash
./gradlew build
```

18 testes: regras de domínio (`Money`, `Order`, `Cart`) + caso de uso de sync com **fakes** das ports — sem Spring, sem banco, sem Mockito. O contexto de teste usa o profile `test` (Liquibase off, `ddl-auto=none`).

## Exemplo de fluxo completo

```bash
curl -X POST localhost:8080/api/catalog/sync                      # sincroniza 194 produtos
curl localhost:8080/api/products?category=smartphones             # lista
curl -X POST localhost:8080/api/customers -H 'Content-Type: application/json' \
     -d '{"name":"Maria","email":"maria@example.com"}'            # cliente (id=1)
curl -X POST localhost:8080/api/cart/1/items -H 'Content-Type: application/json' \
     -d '{"productId":1,"quantity":2}'                            # adiciona ao carrinho
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
     -d '{"customerId":1,"fromCart":true}'                        # compra (esvazia o cart)
curl -X POST localhost:8080/api/orders/1/pay -H 'Content-Type: application/json' \
     -d '{"paymentMethod":"PIX"}'                                 # paga
curl -X POST localhost:8080/api/orders/1/ship                     # envia
curl -X POST localhost:8080/api/orders/1/deliver                  # entrega
```

## Segurança

- `/api/**` está **liberada** durante o desenvolvimento; o resto exige autenticação (HTTP Basic provisório).
- **TODO**: restringir `/api/**` com JWT (`AuthenticationService` + `JwtAuthenticationFilter` no `SecurityConfig`) e desabilitar SpringDoc/Spring Security user auto-generated em produção.

## Próximos passos

1. **JWT**: autenticação real das endpoints de venda.
2. **Estoque**: baixa de `Product.stock` na compra (hoje o estoque é apenas informativo, sincronizado da fonte).
3. **Pagamento externo**: adapter de gateway no lugar do `pay` manual.
