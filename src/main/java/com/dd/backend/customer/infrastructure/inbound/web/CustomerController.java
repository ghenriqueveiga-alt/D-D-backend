package com.dd.backend.customer.infrastructure.inbound.web;

import com.dd.backend.customer.application.port.inbound.GetCustomerUseCase;
import com.dd.backend.customer.application.port.inbound.ListCustomersUseCase;
import com.dd.backend.customer.application.port.inbound.RegisterCustomerUseCase;
import com.dd.backend.customer.domain.model.Customer;
import com.dd.backend.customer.infrastructure.inbound.web.dto.CustomerResponse;
import com.dd.backend.customer.infrastructure.inbound.web.dto.RegisterCustomerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adapter de entrada: cadastro e consulta de clientes. */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Cadastro de clientes da loja")
public class CustomerController {

    private final RegisterCustomerUseCase registerCustomer;
    private final GetCustomerUseCase getCustomer;
    private final ListCustomersUseCase listCustomers;

    @PostMapping
    @Operation(summary = "Cadastra um novo cliente")
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        Customer customer = registerCustomer.execute(request.name(), request.email());
        return ResponseEntity
                .created(URI.create("/api/customers/" + customer.getId()))
                .body(CustomerResponse.from(customer));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um cliente pelo id")
    public CustomerResponse get(@PathVariable Long id) {
        return CustomerResponse.from(getCustomer.execute(id));
    }

    @GetMapping
    @Operation(summary = "Lista todos os clientes")
    public List<CustomerResponse> list() {
        return listCustomers.execute().stream().map(CustomerResponse::from).toList();
    }
}
