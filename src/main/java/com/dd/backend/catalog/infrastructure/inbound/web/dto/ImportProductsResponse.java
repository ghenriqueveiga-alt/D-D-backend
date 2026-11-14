package com.dd.backend.catalog.infrastructure.inbound.web.dto;

import com.dd.backend.catalog.application.port.inbound.ImportProductsUseCase;
import java.util.List;

/** Resposta da importação em lote, com as linhas recusadas e o motivo. */
public record ImportProductsResponse(int created, int updated, List<ItemRejection> rejections) {

    /**
     * @param index      posição do produto na lista enviada pelo cliente
     * @param externalId identificador informado (nulo quando a validação falhou antes)
     * @param message    motivo da recusa
     */
    public record ItemRejection(int index, String externalId, String message) {
    }

    public static ImportProductsResponse from(ImportProductsUseCase.ImportResult result) {
        return new ImportProductsResponse(
                result.created(),
                result.updated(),
                result.rejections().stream()
                        .map(rejection -> new ItemRejection(
                                rejection.index(), rejection.externalId(), rejection.message()))
                        .toList());
    }
}
