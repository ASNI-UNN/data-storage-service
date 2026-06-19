package org.asni.storage.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.asni.storage.domain.model.Dataset;
import org.asni.storage.domain.port.out.DatasetRepository;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
class JdbcDatasetRepository implements DatasetRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    JdbcDatasetRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public Dataset save(Dataset dataset, List<Map<String, String>> rows) {
        Array columnsArray = createSqlArray(dataset.getColumns().toArray(String[]::new));

        jdbc.update(
            "INSERT INTO datasets (id, name, uploaded_by, columns, row_count, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            dataset.getId(),
            dataset.getName(),
            dataset.getUploadedBy(),
            columnsArray,
            dataset.getRowCount(),
            Timestamp.from(dataset.getCreatedAt())
        );

        for (int i = 0; i < rows.size(); i++) {
            PGobject jsonb = toJsonb(rows.get(i));
            jdbc.update(
                "INSERT INTO dataset_rows (id, dataset_id, row_index, data) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), dataset.getId(), i, jsonb
            );
        }

        return dataset;
    }

    @Override
    public Optional<Dataset> findById(UUID id) {
        List<Dataset> result = jdbc.query(
            "SELECT * FROM datasets WHERE id = ?",
            this::mapDataset, id
        );
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<Dataset> findAll() {
        return jdbc.query("SELECT * FROM datasets ORDER BY created_at DESC", this::mapDataset);
    }

    @Override
    public List<Map<String, String>> findRows(UUID datasetId, int offset, int limit) {
        return jdbc.query(
            "SELECT data FROM dataset_rows WHERE dataset_id = ? ORDER BY row_index LIMIT ? OFFSET ?",
            (rs, n) -> fromJsonb(rs.getString("data")),
            datasetId, limit, offset
        );
    }

    @Override
    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM datasets WHERE id = ?", id);
    }

    @Override
    public boolean existsById(UUID id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM datasets WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private Dataset mapDataset(ResultSet rs, int n) throws SQLException {
        Array arr = rs.getArray("columns");
        List<String> columns = arr != null
            ? Arrays.asList((String[]) arr.getArray())
            : List.of();
        return new Dataset(
            rs.getObject("id", UUID.class),
            rs.getString("name"),
            rs.getObject("uploaded_by", UUID.class),
            columns,
            rs.getInt("row_count"),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    private Array createSqlArray(String[] values) {
        try {
            return jdbc.getDataSource().getConnection().createArrayOf("text", values);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private PGobject toJsonb(Map<String, String> data) {
        try {
            PGobject obj = new PGobject();
            obj.setType("jsonb");
            obj.setValue(objectMapper.writeValueAsString(data));
            return obj;
        } catch (JsonProcessingException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> fromJsonb(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
