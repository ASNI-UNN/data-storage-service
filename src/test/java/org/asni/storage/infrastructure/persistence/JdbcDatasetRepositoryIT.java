package org.asni.storage.infrastructure.persistence;

import org.asni.storage.TestcontainersConfig;
import org.asni.storage.domain.model.Dataset;
import org.asni.storage.domain.port.out.DatasetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JdbcDatasetRepository.class, TestcontainersConfig.class})
class JdbcDatasetRepositoryIT {

    @Autowired
    private DatasetRepository repository;

    @Test
    void save_andFindById_roundtrip() {
        UUID userId = UUID.randomUUID();
        List<String> cols = List.of("col1", "col2");
        List<Map<String, String>> rows = List.of(Map.of("col1", "a", "col2", "b"));

        Dataset dataset = Dataset.create("test-dataset", userId, cols, rows.size());
        Dataset saved = repository.save(dataset, rows);

        var found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test-dataset");
        assertThat(found.get().getColumns()).containsExactly("col1", "col2");
        assertThat(found.get().getRowCount()).isEqualTo(1);
    }

    @Test
    void findRows_returnsData() {
        UUID userId = UUID.randomUUID();
        List<String> cols = List.of("name");
        List<Map<String, String>> rows = List.of(
            Map.of("name", "Alice"),
            Map.of("name", "Bob")
        );
        Dataset dataset = Dataset.create("rows-test", userId, cols, rows.size());
        repository.save(dataset, rows);

        List<Map<String, String>> result = repository.findRows(dataset.getId(), 0, 10);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("name", "Alice");
    }

    @Test
    void deleteById_removes() {
        Dataset dataset = Dataset.create("to-delete", UUID.randomUUID(), List.of("x"), 0);
        repository.save(dataset, List.of());

        repository.deleteById(dataset.getId());

        assertThat(repository.findById(dataset.getId())).isEmpty();
    }
}
