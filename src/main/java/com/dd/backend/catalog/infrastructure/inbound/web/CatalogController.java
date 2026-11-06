package com.dd.backend.catalog.infrastructure.inbound.web;

import com.dd.backend.catalog.application.port.inbound.SyncCatalogUseCase;
import com.dd.backend.catalog.infrastructure.inbound.web.dto.SyncResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Adapter de entrada: sincronização do catálogo com a fonte externa. */
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Sincronização com a fonte externa de itens")
public class CatalogController {

    private final SyncCatalogUseCase syncCatalog;

    @PostMapping("/sync")
    @Operation(summary = "Busca todos os itens na internet (DummyJSON) e sincroniza o catálogo local")
    public SyncResponse sync() {
        return new SyncResponse(syncCatalog.execute());
    }
}
