package com.dd.backend.stock.infrastructure.outbound.persistence;

import com.dd.backend.stock.domain.model.StockMovement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mapeamento JPA — não vaza para o domínio (hexagonal). */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
public class StockMovementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_title", nullable = false)
    private String productTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private StockMovement.Type type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 255)
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
