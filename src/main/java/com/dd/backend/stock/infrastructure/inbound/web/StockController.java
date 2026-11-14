package com.dd.backend.stock.infrastructure.inbound.web;

import com.dd.backend.stock.application.port.inbound.GetStockSummaryUseCase;
import com.dd.backend.stock.application.port.inbound.ListStockMovementsUseCase;
import com.dd.backend.stock.application.port.inbound.RegisterStockMovementUseCase;
import com.dd.backend.stock.infrastructure.inbound.web.dto.RegisterStockMovementRequest;
import com.dd.backend.stock.infrastructure.inbound.web.dto.StockMovementResponse;
import com.dd.backend.stock.infrastructure.inbound.web.dto.StockSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Adapter de entrada: movimentações e resumo do estoque. */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Tag(name = "Estoque", description = "Gestão de mercadorias em estoque: movimentações e resumo")
public class StockController {

    private final ListStockMovementsUseCase listMovements;
    private final RegisterStockMovementUseCase registerMovement;
    private final GetStockSummaryUseCase getSummary;

    @GetMapping("/movements")
    @Operation(summary = "Lista movimentações de estoque (filtro opcional por produto)")
    public List<StockMovementResponse> list(@RequestParam(name = "productId", required = false) Long productId) {
        return listMovements.execute(productId).stream().map(StockMovementResponse::from).toList();
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma entrada ou saída de mercadoria do estoque")
    public StockMovementResponse register(@Valid @RequestBody RegisterStockMovementRequest request) {
        return StockMovementResponse.from(registerMovement.execute(request.toCommand()));
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumo consolidado do estoque (unidades, valor, faltas e movimentações)")
    public StockSummaryResponse summary() {
        return StockSummaryResponse.from(getSummary.execute());
    }
}
