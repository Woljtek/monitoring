CREATE TABLE external_input
(
    id bigserial PRIMARY KEY,
    filename VARCHAR NOT NULL UNIQUE,
    mission VARCHAR,
    pickup_point_seen_date TIMESTAMP,
    pickup_point_available_date TIMESTAMP,
    ingestion_date TIMESTAMP,
    catalog_storage_date TIMESTAMP,
    custom json
);

CREATE TABLE dsib
(
    parent_id bigint NOT NULL REFERENCES external_input(id) ON DELETE CASCADE
);

CREATE TABLE chunk
(
    parent_id bigint NOT NULL REFERENCES external_input(id) ON DELETE CASCADE
);

CREATE TABLE aux_data
(
    parent_id bigint NOT NULL REFERENCES external_input(id) ON DELETE CASCADE
);
