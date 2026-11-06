package com.dd.backend.catalog.infrastructure.outbound.external.dto;

import java.util.List;

public record DummyJsonProductListResponse(int total, List<DummyJsonProductDto> products) {
}
