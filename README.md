# D-D Backend

Backend de um sistema de venda de itens, gerado com [start.spring.io](https://start.spring.io).

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

## Dependências

- `spring-boot-starter-webmvc` — API REST
- `spring-boot-starter-data-jpa` — persistência
- `spring-boot-starter-validation` — validação de DTOs/entidades
- `spring-boot-starter-security` — segurança
- `spring-boot-starter-liquibase` — controle de migrations
- `postgresql` — driver do banco
- `lombok` — redução de boilerplate
- `springdoc-openapi-starter-webmvc-ui:3.2.0` — Swagger UI
- `jjwt-api/impl/jackson:0.13.0` — emissão/validação de tokens JWT

## Requisitos

- JDK 21+ (o Gradle baixa automaticamente via foojay resolver, se necessário)
- PostgreSQL rodando em `localhost:5432`

## Como rodar

```bash
# banco (uma vez)
createdb dd_store   # ou crie manualmente

./gradlew bootRun
```

A aplicação sobe em `http://localhost:8080`.

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

Os testes rodam sem banco (Liquibase desabilitado em `src/test/resources/application.properties`).

## Endpoints

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

> A API está protegida por HTTP Basic por padrão (usuário `user` com a senha exibida no console ao iniciar). A documentação Swagger fica liberada.

## Estrutura

```
src/main/java/com/dd/backend/
├── DDBackendApplication.java
└── config/SecurityConfig.java        # security stateless (JWT a conectar)
src/main/resources/
├── application.properties            # configuração geral
└── db/changelog/db.changelog-master.yaml   # changelog do Liquibase
```

## Próximos passos

1. **JWT**: implementar `AuthenticationService` + filtro `JwtAuthenticationFilter` no `SecurityConfig`.
2. **Módulos de venda**: `item`, `cliente`, `pedido/venda`, `pagamento` (pacotes `domain`, `dto`, `repository`, `service`, `controller`).
3. **Migrations**: criar as tabelas em `db/changelog/changes/` e incluí-las no changelog master.
