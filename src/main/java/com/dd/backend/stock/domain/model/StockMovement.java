package com.dd.backend.stock.domain.model;

import com.dd.backend.shared.domain.DomainException;
import java.time.Instant;
import java.util.Objects;

/**
 * Movimentação de estoque (entrada ou saída) de uma mercadoria.
 *
 * <p>A movimentação é o registro imutável do que aconteceu com o saldo: o
 * saldo atual fica no produto do catálogo, o histórico fica aqui. O título do
 * produto é gravado como snapshot para o histórico continuar legível mesmo se
 * o produto for renomeado.</p>
 */
public final class StockMovement {

    /** Tipo da movimentação: entrada soma no saldo, saída subtrai. */
    public enum Type {
        IN,
        OUT
    }

    private Long id;
    private Long productId;
    private String productTitle;
    private Type type;
    private int quantity;
    private String note;
    private Instant occurredAt;

    private StockMovement() {
    }

    /** Registra uma nova movimentação (sem id — atribuído pela persistência). */
    public static StockMovement register(Long productId, String productTitle, Type type,
                                         int quantity, String note) {
        if (productId == null) {
            throw new DomainException("Produto é obrigatório para a movimentação de estoque");
        }
        if (productTitle == null || productTitle.isBlank()) {
            throw new DomainException("Título do produto é obrigatório para a movimentação");
        }
        if (type == null) {
            throw new DomainException("Tipo da movimentação é obrigatório (entrada ou saída)");
        }
        if (quantity <= 0) {
            throw new DomainException("Quantidade da movimentação deve ser maior que zero");
        }
        if (note != null && note.length() > 255) {
            throw new DomainException("Observação da movimentação deve ter no máximo 255 caracteres");
        }
        StockMovement movement = new StockMovement();
        movement.productId = productId;
        movement.productTitle = productTitle.trim();
        movement.type = type;
        movement.quantity = quantity;
        movement.note = note == null || note.isBlank() ? null : note.trim();
        movement.occurredAt = Instant.now();
        return movement;
    }

    /** Reconstituição a partir do banco (uso exclusivo da camada de persistência). */
    public static StockMovement restore(Long id, Long productId, String productTitle, Type type,
                                        int quantity, String note, Instant occurredAt) {
        StockMovement movement = register(productId, productTitle, type, quantity, note);
        movement.id = Objects.requireNonNull(id, "id é obrigatório na reconstituição");
        movement.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt é obrigatório na reconstituição");
        return movement;
    }

    /** Atribuído pelo adapter de persistência após o INSERT. */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new DomainException("ID da movimentação já foi atribuído");
        }
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public Type getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getNote() {
        return note;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
