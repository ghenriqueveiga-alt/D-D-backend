package com.dd.backend.order.domain.model;

import com.dd.backend.shared.domain.DomainException;

/**
 * Value object de endereço de entrega do pedido (checkout).
 *
 * <p>Normaliza os campos (trim, CEP com máscara, UF em maiúsculas) e valida o
 * formato brasileiro no momento da criação — qualquer problema vira
 * {@link DomainException} e a API responde 422.</p>
 */
public record ShippingAddress(
        String recipientName,
        String phone,
        String cep,
        String street,
        String number,
        String complement,
        String city,
        String uf) {

    public ShippingAddress {
        recipientName = requireText(recipientName, "Nome do destinatário", 255);
        phone = normalizePhone(phone);
        cep = normalizeCep(cep);
        street = requireText(street, "Rua ou avenida", 255);
        number = requireText(number, "Número do endereço", 16);
        complement = complement == null || complement.isBlank() ? null : complement.trim();
        city = requireText(city, "Cidade", 128);
        uf = normalizeUf(uf);
    }

    /** Cria o endereço a partir dos campos crus, normalizando tudo. */
    public static ShippingAddress of(String recipientName, String phone, String cep, String street,
                                     String number, String complement, String city, String uf) {
        return new ShippingAddress(recipientName, phone, cep, street, number, complement, city, uf);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " é obrigatório");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new DomainException(field + " deve ter no máximo " + maxLength + " caracteres");
        }
        return trimmed;
    }

    private static String normalizePhone(String value) {
        String phone = requireText(value, "Telefone", 32);
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 13) {
            throw new DomainException("Telefone inválido (informe DDD + número, ex.: 11 99999-9999)");
        }
        return phone;
    }

    private static String normalizeCep(String value) {
        String cep = requireText(value, "CEP", 9);
        String digits = cep.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new DomainException("CEP inválido (informe 8 dígitos, ex.: 01310-100)");
        }
        return digits.substring(0, 5) + "-" + digits.substring(5);
    }

    private static String normalizeUf(String value) {
        String uf = requireText(value, "UF", 2);
        if (!uf.matches("[A-Za-z]{2}")) {
            throw new DomainException("UF inválida (use a sigla do estado, ex.: SP)");
        }
        return uf.toUpperCase();
    }
}
