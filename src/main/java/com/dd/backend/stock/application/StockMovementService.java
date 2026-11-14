package com.dd.backend.stock.application;

import com.dd.backend.catalog.application.port.inbound.AdjustStockUseCase;
import com.dd.backend.catalog.domain.model.Product;
import com.dd.backend.shared.domain.DomainException;
import com.dd.backend.stock.application.port.inbound.RegisterStockMovementUseCase;
import com.dd.backend.stock.domain.model.StockMovement;
import com.dd.backend.stock.domain.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Caso de uso de movimentação de estoque.
 *
 * <p>Primeiro valida o comando, depois ajusta o saldo do produto via porta do
 * catálogo ({@link AdjustStockUseCase}) e só então grava o registro no
 * histórico — assim uma saída acima do saldo é recusada antes de qualquer
 * efeito colateral.</p>
 */
@Service
@RequiredArgsConstructor
public class StockMovementService implements RegisterStockMovementUseCase {

    private final StockMovementRepository movementRepository;
    private final AdjustStockUseCase adjustStock;

    @Override
    public StockMovement execute(Command command) {
        if (command == null || command.productId() == null) {
            throw new DomainException("Produto é obrigatório para a movimentação de estoque");
        }
        if (command.type() == null) {
            throw new DomainException("Tipo da movimentação é obrigatório (entrada ou saída)");
        }
        if (command.quantity() <= 0) {
            throw new DomainException("Quantidade da movimentação deve ser maior que zero");
        }

        int delta = command.type() == StockMovement.Type.IN
                ? command.quantity()
                : -command.quantity();

        Product product = adjustStock.execute(command.productId(), delta);
        StockMovement movement = StockMovement.register(
                product.getId(), product.getTitle(), command.type(), command.quantity(), command.note());
        return movementRepository.save(movement);
    }
}
