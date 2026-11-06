package com.dd.backend.cart.infrastructure.outbound.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartJpaRepository extends JpaRepository<CartJpaEntity, Long> {

    Optional<CartJpaEntity> findByCustomerId(Long customerId);
}
