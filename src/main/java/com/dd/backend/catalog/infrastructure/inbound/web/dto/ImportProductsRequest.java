package com.dd.backend.catalog.infrastructure.inbound.web.dto;

import com.dd.backend.catalog.application.port.inbound.ImportProductsUseCase.ProductEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * Payload da importação em lote: uma lista de produtos convertida dos arquivos
 * (CSV/JSON) no cliente.
 *
 * <p>A validação abaixo é a primeira linha de defesa; o caso de uso revalida
 * pelo domínio antes de persistir.</p>
 */
public record ImportProductsRequest(
        @NotEmpty(message = "lista de produtos não pode ser vazia") List<@Valid ProductRequest> products) {

    public record ProductRequest(
            @NotBlank(message = "identificador externo é obrigatório") String externalId,
            @NotBlank(message = "título é obrigatório") String title,
            String description,
            @NotNull(message = "preço é obrigatório")
            @DecimalMin(value = "0", message = "preço não pode ser negativo") BigDecimal price,
            String category,
            String brand,
            String imageUrl,
            @NotNull(message = "estoque é obrigatório")
            @Min(value = 0, message = "estoque não pode ser negativo") Integer stock) {

        public ProductEntry toEntry() {
            return new ProductEntry(externalId, title, description, price, category, brand, imageUrl, stock);
        }
    }
}
