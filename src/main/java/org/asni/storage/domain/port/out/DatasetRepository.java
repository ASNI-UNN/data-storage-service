package org.asni.storage.domain.port.out;

import org.asni.storage.domain.model.Dataset;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface DatasetRepository {
    Dataset save(Dataset dataset, List<Map<String, String>> rows);
    Optional<Dataset> findById(UUID id);
    List<Dataset> findAll();
    List<Map<String, String>> findRows(UUID datasetId, int offset, int limit);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
