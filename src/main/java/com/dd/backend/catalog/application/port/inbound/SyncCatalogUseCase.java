package com.dd.backend.catalog.application.port.inbound;

/** Porta de entrada: sincroniza o catálogo local com a fonte externa de itens. */
public interface SyncCatalogUseCase {

    /** Executa a sincronização e retorna a quantidade de produtos processados. */
    int execute();
}
