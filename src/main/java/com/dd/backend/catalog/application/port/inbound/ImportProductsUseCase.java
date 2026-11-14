package com.dd.backend.catalog.application.port.inbound;

import java.math.BigDecimal;
import java.util.List;

/**
 * Porta de entrada: importa vários produtos em lote, a partir dos arquivos
 * (CSV/JSON) enviados pelo cliente e convertidos em entradas.
 *
 * <p>Cada entrada é validada isoladamente: as válidas são importadas (upsert
 * por {@code externalId}) e as recusadas voltam com o motivo e a posição na
 * lista enviada, para o cliente apontar a linha exata no arquivo.</p>
 */
public interface ImportProductsUseCase {

    /** Executa a importação e devolve o resumo da operação. */
    ImportResult execute(List<ProductEntry> entries);

    /** Dados de um produto a importar. */
    record ProductEntry(
            String externalId,
            String title,
            String description,
            BigDecimal price,
            String category,
            String brand,
            String imageUrl,
            Integer stock) {
    }

    /** Resumo da importação: novos, atualizados e recusados. */
    record ImportResult(int created, int updated, List<Rejection> rejections) {
    }

    /**
     * @param index      posição da entrada na lista enviada (base 0)
     * @param externalId identificador informado (pode ser nulo em falha de validação)
     * @param message    motivo da recusa
     */
    record Rejection(int index, String externalId, String message) {
    }
}
