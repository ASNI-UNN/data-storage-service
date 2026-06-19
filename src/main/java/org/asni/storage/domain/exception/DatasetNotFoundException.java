package org.asni.storage.domain.exception;

import java.util.UUID;

public class DatasetNotFoundException extends RuntimeException {
    public DatasetNotFoundException(UUID id) {
        super("Dataset not found: " + id);
    }
}
