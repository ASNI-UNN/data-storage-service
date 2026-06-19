--liquibase formatted sql

--changeset asni:1-init
CREATE TABLE datasets (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    uploaded_by UUID         NOT NULL,
    columns     TEXT[]       NOT NULL DEFAULT '{}',
    row_count   INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE dataset_rows (
    id         UUID PRIMARY KEY,
    dataset_id UUID NOT NULL REFERENCES datasets(id) ON DELETE CASCADE,
    row_index  INT  NOT NULL,
    data       JSONB NOT NULL,
    UNIQUE(dataset_id, row_index)
);

CREATE INDEX idx_dataset_rows_dataset_id ON dataset_rows(dataset_id);
