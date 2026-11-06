package com.dd.backend.catalog.domain.model;

import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.shared.domain.Money;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate de produto do catálogo.
 *
 * <p>Os produtos são sincronizados de uma fonte externa (DummyJSON) e vendidos
 * localmente. O {@code externalId} é a identidade na fonte externa; o {@code id}
 * é a identidade local.</p>
 */
public final class Product {

    private Long id;
    private String externalId;
    private String title;
    private String description;
    private Money price;
    private String category;
    private String brand;
    private String imageUrl;
    private int stock;
    private Instant createdAt;

    private Product() {
    }

    /** Cria um produto novo a partir de um item sincronizado da fonte externa. */
    public static Product sync(String externalId, String title, String description, Money price,
                               String category, String brand, String imageUrl, int stock) {
        validate(externalId, title, price, stock);
        Product product = new Product();
        product.externalId = externalId.trim();
        product.title = title.trim();
        product.description = description;
        product.price = price;
        product.category = category;
        product.brand = brand;
        product.imageUrl = imageUrl;
        product.stock = stock;
        product.createdAt = Instant.now();
        return product;
    }

    /** Reconstituição a partir do banco (uso exclusivo da camada de persistência). */
    public static Product restore(Long id, String externalId, String title, String description, Money price,
                                  String category, String brand, String imageUrl, int stock, Instant createdAt) {
        Product product = sync(externalId, title, description, price, category, brand, imageUrl, stock);
        product.id = Objects.requireNonNull(id, "id é obrigatório na reconstituição");
        product.createdAt = Objects.requireNonNull(createdAt, "createdAt é obrigatório na reconstituição");
        return product;
    }

    /** Atualiza os dados sincronizados mantendo a identidade local. */
    public Product update(String title, String description, Money price,
                          String category, String brand, String imageUrl, int stock) {
        validate(this.externalId, title, price, stock);
        this.title = title.trim();
        this.description = description;
        this.price = price;
        this.category = category;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.stock = stock;
        return this;
    }

    private static void validate(String externalId, String title, Money price, int stock) {
        if (externalId == null || externalId.isBlank()) {
            throw new DomainException("Identificador externo do produto é obrigatório");
        }
        if (title == null || title.isBlank()) {
            throw new DomainException("Título do produto é obrigatório");
        }
        if (price == null) {
            throw new DomainException("Preço do produto é obrigatório");
        }
        if (stock < 0) {
            throw new DomainException("Estoque do produto não pode ser negativo");
        }
    }

    /** Atribuído pelo adapter de persistência após o INSERT. */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new DomainException("ID do produto já foi atribuído");
        }
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Money getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getStock() {
        return stock;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
