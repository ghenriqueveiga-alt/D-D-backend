# D-D Backend

API de vendas de itens reais buscados na internet ([DummyJSON](https://dummyjson.com)), construída com **DDD + arquitetura hexagonal** (ports & adapters).

## Stack

| Item | Tecnologia |
|---|---|
| Framework | Spring Boot **4.1.2** |
| Linguagem | Java **21** (toolchain) |
| Build | Gradle 9 (Kotlin DSL) |
| Banco | PostgreSQL (dev/prod) · H2 em memória (test) |
| Migrações | Liquibase (perfil `prod`) |
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
├── order/                           # contexto: pedidos (aggregate Order)
│   └── domain/model/Order           # ciclo de vida PENDING→PAID→SHIPPED→DELIVERED
│                                    # (e cancel em PENDING/PAID)
└── stock/                           # contexto: gestão de estoque (movimentações)
    └── domain/model/StockMovement   # registro imutável IN/OUT; o saldo vive em Product.stock
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
| `POST` | `/api/products/import` | Importa **vários produtos em lote** (`{"products":[...]}` — upsert por `externalId`; linhas inválidas voltam em `rejections`, sem interromper as demais) |
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

### Estoque (gestão de mercadorias)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/stock/movements` | Lista movimentações, mais recentes primeiro (filtro opcional `?productId=1`) |
| `POST` | `/api/stock/movements` | Registra entrada/saída: `{"productId":1,"type":"IN","quantity":10,"note":"reposição"}` — `type` é `IN` ou `OUT`; saída acima do saldo → `422` |
| `GET` | `/api/stock/summary` | Resumo: SKUs, unidades, valor em estoque, itens em falta, estoque baixo (≤5) e total de movimentações |

### Códigos de erro (ProblemDetail RFC 7807)

| HTTP | Quando |
|---|---|
| `400` | JSON inválido ou validação de campos (`errors` no corpo) |
| `404` | Recurso inexistente (produto/cliente/pedido) |
| `409` | Violação de restrição única (e-mail duplicado) |
| `422` | Regra de negócio violada (transição de status inválida, carrinho vazio…) |
| `502` | Falha ao consultar a fonte externa (DummyJSON) |

## Como rodar

### Perfis de execução

O perfil padrão é o `dev` (`spring.profiles.default=dev` em `application.properties`); escolha outro com `--spring.profiles.active=<perfil>`:

| Perfil | Banco | Schema | Console |
|---|---|---|---|
| `dev` (padrão) | PostgreSQL (`localhost:5432/dd_store`) | Hibernate (`ddl-auto=update`), Liquibase **desligado** | — |
| `prod` | PostgreSQL (`localhost:5432/dd_store`) | Liquibase executa as migrations e Hibernate só **valida** (`ddl-auto=validate`) | — |
| `test` | H2 em memória (`jdbc:h2:mem:testdb`, modo PostgreSQL) | Hibernate (`ddl-auto=create-drop`), Liquibase desligado | [`/h2-console`](http://localhost:8080/h2-console) liberado (JDBC URL `jdbc:h2:mem:testdb`, usuário `sa`) |

Arquivos: `application.properties` (comum) + `application-dev.properties`, `application-prod.properties`, `application-test.properties`.

> **Spring Boot 4**: a autoconfiguração do console H2 saiu do `spring-boot-autoconfigure` e vive no módulo `spring-boot-h2console` (declarado no `build.gradle.kts`); sem ele, `/h2-console` responde 404 mesmo com `spring.h2.console.enabled=true`.

```bash
./gradlew bootRunDev     # dev (padrão) — PostgreSQL, porta 8090
./gradlew bootRunProd    # prod — Liquibase + PostgreSQL, porta 8092
./gradlew bootRunTest    # test — H2 + h2-console, porta 8091 → http://localhost:8091/h2-console
# porta alternativa: ./gradlew bootRunTest -Pport=9091
```

**No IntelliJ**: painel *Gradle* → `D-D-backend` → `Tasks` → `application` → duplo clique em `bootRunDev` / `bootRunTest` / `bootRunProd` (botão direito → *Run* salva uma Run Configuration reutilizável).

> `bootRun` sem argumentos continua funcionando (perfil `dev` por padrão).

> Em `test`, o H2 também é o banco dos testes JUnit (`@ActiveProfiles("test")`).

### 1. Banco (PostgreSQL — perfis dev/prod)

```bash
docker run -d --name dd-postgres \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=dd_store -p 5432:5432 postgres:16-alpine
```

As migrations do Liquibase (`src/main/resources/db/changelog/changes/`) executam automaticamente na subida **do perfil `prod`**; no `dev` o schema é gerido pelo Hibernate (`ddl-auto=update`).

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

38 testes: regras de domínio (`Money`, `Order`, `Cart`) + casos de uso do catálogo (sync, importação em lote e ajuste de estoque) e do estoque (movimentações e resumo) com **fakes** das ports — sem Spring, sem Mockito. O contexto de teste usa o profile `test` (H2 em memória, Liquibase off).

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
- `/h2-console/**` está liberado (apenas relevante no perfil `test`, onde o console é habilitado).
- **TODO**: restringir `/api/**` com JWT (`AuthenticationService` + `JwtAuthenticationFilter` no `SecurityConfig`) e desabilitar SpringDoc/Spring Security user auto-generated em produção.

## Próximos passos

1. **JWT**: autenticação real das endpoints de venda.
2. **Estoque**: baixa automática de `Product.stock` na compra (hoje o estoque é ajustado manualmente pelas movimentações `IN`/`OUT`).
3. **Pagamento externo**: adapter de gateway no lugar do `pay` manual.
