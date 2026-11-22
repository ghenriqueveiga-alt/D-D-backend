package com.dd.backend.order.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dd.backend.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

/** Normalização e validação do endereço de entrega (checkout). */
class ShippingAddressTest {

    private static ShippingAddress build(String recipient, String phone, String cep,
                                         String street, String number, String complement,
                                         String city, String uf) {
        return ShippingAddress.of(recipient, phone, cep, street, number, complement, city, uf);
    }

    @Test
    void normalizaCepComMascaraEUfEmMaiusculas() {
        ShippingAddress address = build(
                "  Maria da Silva ", "11 99999-9999", "01310100",
                " Avenida Paulista ", "1000", "  ", "São Paulo", "sp");

        assertEquals("Maria da Silva", address.recipientName());
        assertEquals("01310-100", address.cep());
        assertEquals("Avenida Paulista", address.street());
        assertEquals("SP", address.uf());
        assertNull(address.complement(), "complemento em branco vira null");
    }

    @Test
    void camposObrigatoriosAusentesSaoRecusados() {
        assertThrows(DomainException.class,
                () -> build(null, "11 99999-9999", "01310-100", "Rua A", "1", null, "São Paulo", "SP"));
        assertThrows(DomainException.class,
                () -> build("Maria", null, "01310-100", "Rua A", "1", null, "São Paulo", "SP"));
        assertThrows(DomainException.class,
                () -> build("Maria", "11 99999-9999", "01310-100", " ", "1", null, "São Paulo", "SP"));
        assertThrows(DomainException.class,
                () -> build("Maria", "11 99999-9999", "01310-100", "Rua A", null, null, "São Paulo", "SP"));
        assertThrows(DomainException.class,
                () -> build("Maria", "11 99999-9999", "01310-100", "Rua A", "1", null, null, "SP"));
        assertThrows(DomainException.class,
                () -> build("Maria", "11 99999-9999", "01310-100", "Rua A", "1", null, "São Paulo", null));
    }

    @Test
    void cepETelefoneComFormatoErradoSaoRecusados() {
        assertThrows(DomainException.class,
                () -> build("Maria", "11 99999-9999", "123", "Rua A", "1", null, "São Paulo", "SP"));
        assertThrows(DomainException.class,
                () -> build("Maria", "12345", "01310-100", "Rua A", "1", null, "São Paulo", "SP"));
        assertThrows(DomainException.class,
                () -> build("Maria", "11 99999-9999", "01310-100", "Rua A", "1", null, "São Paulo", "S1"));
    }
}
