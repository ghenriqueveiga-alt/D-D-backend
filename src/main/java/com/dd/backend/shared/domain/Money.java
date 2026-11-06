package com.dd.backend.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object monetário (domain kernel).
 * Imutável, sempre com escala 2 e arredondamento HALF_UP.
 */
public final class Money {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount;
    }

    public static Money of(BigDecimal amount) {
        Objects.requireNonNull(amount, "valor monetário não pode ser nulo");
        if (amount.signum() < 0) {
            throw new DomainException("Valor monetário não pode ser negativo: " + amount);
        }
        return new Money(amount.setScale(SCALE, ROUNDING));
    }

    public static Money of(String amount) {
        return of(new BigDecimal(amount));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO.setScale(SCALE, ROUNDING));
    }

    public Money plus(Money other) {
        Objects.requireNonNull(other, "valor a somar não pode ser nulo");
        return new Money(this.amount.add(other.amount).setScale(SCALE, ROUNDING));
    }

    public Money times(int quantity) {
        if (quantity < 1) {
            throw new DomainException("Quantidade deve ser maior que zero");
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)).setScale(SCALE, ROUNDING));
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public BigDecimal amount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money other)) {
            return false;
        }
        return amount.compareTo(other.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
