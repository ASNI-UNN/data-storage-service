package org.asni.storage.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Dataset {

    private final UUID id;
    private final String name;
    private final UUID uploadedBy;
    private final List<String> columns;
    private final int rowCount;
    private final Instant createdAt;

    public Dataset(UUID id, String name, UUID uploadedBy, List<String> columns, int rowCount, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.uploadedBy = uploadedBy;
        this.columns = List.copyOf(columns);
        this.rowCount = rowCount;
        this.createdAt = createdAt;
    }

    public static Dataset create(String name, UUID uploadedBy, List<String> columns, int rowCount) {
        return new Dataset(UUID.randomUUID(), name, uploadedBy, columns, rowCount, Instant.now());
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getUploadedBy() { return uploadedBy; }
    public List<String> getColumns() { return columns; }
    public int getRowCount() { return rowCount; }
    public Instant getCreatedAt() { return createdAt; }
}
