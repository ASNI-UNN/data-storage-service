package org.asni.storage.domain.port.in;

import org.asni.storage.domain.model.Dataset;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DatasetUseCase {
    Dataset upload(String name, UUID uploadedBy, List<String> columns, List<Map<String, String>> rows);
    Dataset findById(UUID id);
    List<Dataset> findAll();
    List<Map<String, String>> getRows(UUID datasetId, int page, int size);
    void delete(UUID id);
}
