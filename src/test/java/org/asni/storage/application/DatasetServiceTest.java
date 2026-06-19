package org.asni.storage.application;

import org.asni.storage.application.service.DatasetService;
import org.asni.storage.domain.exception.DatasetNotFoundException;
import org.asni.storage.domain.model.Dataset;
import org.asni.storage.domain.port.out.DatasetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest {

    @Mock
    private DatasetRepository repository;

    @InjectMocks
    private DatasetService service;

    @Test
    void upload_savesAndReturnsDataset() {
        UUID userId = UUID.randomUUID();
        List<String> cols = List.of("name", "age");
        List<Map<String, String>> rows = List.of(Map.of("name", "Alice", "age", "30"));

        Dataset expected = new Dataset(UUID.randomUUID(), "test", userId, cols, 1, Instant.now());
        when(repository.save(any(), eq(rows))).thenReturn(expected);

        Dataset result = service.upload("test", userId, cols, rows);

        assertThat(result.getName()).isEqualTo("test");
        assertThat(result.getRowCount()).isEqualTo(1);
    }

    @Test
    void findById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
            .isInstanceOf(DatasetNotFoundException.class);
    }

    @Test
    void delete_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id))
            .isInstanceOf(DatasetNotFoundException.class);
    }

    @Test
    void delete_existing_callsRepo() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        service.delete(id);

        verify(repository).deleteById(id);
    }
}
