package com.dd.backend.customer.domain.model;

import com.dd.backend.shared.domain.DomainException;
import java.time.Instant;
import java.util.Objects;

/** Aggregate de cliente. */
public final class Customer {

    private Long id;
    private String name;
    private String email;
    private Instant createdAt;

    private Customer() {
    }

    public static Customer register(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Nome do cliente é obrigatório");
        }
        Customer customer = new Customer();
        customer.name = name.trim();
        customer.email = normalizeEmail(email);
        customer.createdAt = Instant.now();
        return customer;
    }

    /** Reconstituição a partir do banco (uso exclusivo da camada de persistência). */
    public static Customer restore(Long id, String name, String email, Instant createdAt) {
        Customer customer = register(name, email);
        customer.id = Objects.requireNonNull(id, "id é obrigatório na reconstituição");
        customer.createdAt = Objects.requireNonNull(createdAt, "createdAt é obrigatório na reconstituição");
        return customer;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new DomainException("E-mail do cliente é obrigatório");
        }
        String normalized = email.trim().toLowerCase();
        if (!normalized.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new DomainException("E-mail inválido: " + email);
        }
        return normalized;
    }

    /** Atribuído pelo adapter de persistência após o INSERT. */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new DomainException("ID do cliente já foi atribuído");
        }
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
