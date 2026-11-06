package com.dd.backend.shared.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void deveArredondarParaDuasCasasComHalfUp() {
        assertEquals(new BigDecimal("10.50"), Money.of("10.5").amount());
        assertEquals(new BigDecimal("0.01"), Money.of("0.005").amount());
        assertEquals(new BigDecimal("0.02"), Money.of("0.015").amount());
    }

    @Test
    void naoAceitaValorNegativo() {
        assertThrows(DomainException.class, () -> Money.of("-1.00"));
    }

    @Test
    void deveSomarEMultiplicar() {
        Money a = Money.of("10.50");
        Money b = Money.of("2.25");

        assertEquals(new BigDecimal("12.75"), a.plus(b).amount());
        assertEquals(new BigDecimal("21.00"), a.times(2).amount());
        assertTrue(Money.zero().plus(b).isPositive());
    }

    @Test
    void multiplicarPorZeroERejeitado() {
        assertThrows(DomainException.class, () -> Money.of("10.00").times(0));
    }
}
