package org.asni.storage.infrastructure.web.dto;

import org.asni.storage.domain.model.Dataset;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DatasetResponse(
    UUID id,
    String name,
    UUID uploadedBy,
    List<String> columns,
    int rowCount,
    Instant createdAt
) {
    public static DatasetResponse from(Dataset d) {
        return new DatasetResponse(d.getId(), d.getName(), d.getUploadedBy(), d.getColumns(), d.getRowCount(), d.getCreatedAt());
    }
}
