package org.asni.storage.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.asni.storage.domain.port.in.DatasetUseCase;
import org.asni.storage.infrastructure.excel.ExcelParser;
import org.asni.storage.infrastructure.web.dto.DatasetResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/datasets")
@Tag(name = "Datasets", description = "Upload and manage Excel datasets")
class DatasetController {

    private final DatasetUseCase datasetUseCase;
    private final ExcelParser excelParser;

    DatasetController(DatasetUseCase datasetUseCase, ExcelParser excelParser) {
        this.datasetUseCase = datasetUseCase;
        this.excelParser = excelParser;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Excel file as dataset")
    ResponseEntity<DatasetResponse> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam("name") String name,
        Authentication auth
    ) {
        UUID userId = (UUID) auth.getPrincipal();
        ExcelParser.ParsedExcel parsed = excelParser.parse(file);
        var dataset = datasetUseCase.upload(name, userId, parsed.columns(), parsed.rows());
        return ResponseEntity.status(HttpStatus.CREATED).body(DatasetResponse.from(dataset));
    }

    @GetMapping
    @Operation(summary = "List all datasets")
    ResponseEntity<List<DatasetResponse>> findAll() {
        List<DatasetResponse> list = datasetUseCase.findAll().stream().map(DatasetResponse::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dataset metadata")
    ResponseEntity<DatasetResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(DatasetResponse.from(datasetUseCase.findById(id)));
    }

    @GetMapping("/{id}/rows")
    @Operation(summary = "Get dataset rows (paginated)")
    ResponseEntity<List<Map<String, String>>> getRows(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(datasetUseCase.getRows(id, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete dataset")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        datasetUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
