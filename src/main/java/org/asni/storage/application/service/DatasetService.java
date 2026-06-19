package org.asni.storage.application.service;

import org.asni.storage.domain.exception.DatasetNotFoundException;
import org.asni.storage.domain.model.Dataset;
import org.asni.storage.domain.port.in.DatasetUseCase;
import org.asni.storage.domain.port.out.DatasetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class DatasetService implements DatasetUseCase {

    private final DatasetRepository repository;

    public DatasetService(DatasetRepository repository) {
        this.repository = repository;
    }

    @Override
    public Dataset upload(String name, UUID uploadedBy, List<String> columns, List<Map<String, String>> rows) {
        Dataset dataset = Dataset.create(name, uploadedBy, columns, rows.size());
        return repository.save(dataset, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Dataset findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new DatasetNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dataset> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> getRows(UUID datasetId, int page, int size) {
        if (!repository.existsById(datasetId)) {
            throw new DatasetNotFoundException(datasetId);
        }
        return repository.findRows(datasetId, page * size, size);
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new DatasetNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
