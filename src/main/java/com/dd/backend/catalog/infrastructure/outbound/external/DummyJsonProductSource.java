package com.dd.backend.catalog.infrastructure.outbound.external;

import com.dd.backend.catalog.application.port.outbound.ProductSource;
import com.dd.backend.catalog.infrastructure.outbound.external.dto.DummyJsonProductDto;
import com.dd.backend.catalog.infrastructure.outbound.external.dto.DummyJsonProductListResponse;
import com.dd.backend.shared.domain.ExternalServiceException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter de saída que consulta o DummyJSON (https://dummyjson.com).
 * Trocar a fonte de itens = implementar {@link ProductSource} em outro adapter.
 */
@Component
public class DummyJsonProductSource implements ProductSource {

    private static final String BASE_URL = "https://dummyjson.com";

    private final RestClient restClient;

    public DummyJsonProductSource(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    @Override
    public List<ExternalProduct> fetchAll() {
        try {
            DummyJsonProductListResponse response = restClient.get()
                    .uri("/products?limit=0")
                    .retrieve()
                    .body(DummyJsonProductListResponse.class);
            if (response == null || response.products() == null) {
                return List.of();
            }
            return response.products().stream()
                    .filter(dto -> dto.id() != null && dto.title() != null)
                    .map(this::toExternalProduct)
                    .toList();
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Falha ao buscar itens em " + BASE_URL, ex);
        }
    }

    private ExternalProduct toExternalProduct(DummyJsonProductDto dto) {
        return new ExternalProduct(
                String.valueOf(dto.id()),
                dto.title(),
                dto.description(),
                dto.price(),
                dto.category(),
                dto.brand(),
                resolveImageUrl(dto),
                dto.stock() == null ? 0 : dto.stock());
    }

    private String resolveImageUrl(DummyJsonProductDto dto) {
        if (dto.thumbnail() != null && !dto.thumbnail().isBlank()) {
            return dto.thumbnail();
        }
        if (dto.images() != null && !dto.images().isEmpty()) {
            return dto.images().get(0);
        }
        return null;
    }
}
