package com.dd.backend.stock.infrastructure.inbound.web.dto;

import com.dd.backend.stock.application.port.inbound.RegisterStockMovementUseCase.Command;
import com.dd.backend.stock.domain.model.StockMovement.Type;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload de registro de movimentação: produto, tipo (IN/OUT), quantidade e
 * observação opcional.
 *
 * <p>A validação abaixo é a primeira linha de defesa; o caso de uso revalida
 * pelo domínio antes de tocar no saldo.</p>
 */
public record RegisterStockMovementRequest(
        @NotNull(message = "produto é obrigatório") Long productId,
        @NotNull(message = "tipo é obrigatório (IN ou OUT)") Type type,
        @Min(value = 1, message = "quantidade deve ser maior que zero") int quantity,
        @Size(max = 255, message = "observação deve ter no máximo 255 caracteres") String note) {

    public Command toCommand() {
        return new Command(productId, type, quantity, note);
    }
}
