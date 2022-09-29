CREATE TABLE external_input
(
    id bigserial PRIMARY KEY,
    filename VARCHAR NOT NULL UNIQUE,
    mission VARCHAR,
    pickup_point_seen_date TIMESTAMP,
    pickup_point_available_date TIMESTAMP,
    ingestion_date TIMESTAMP,
    catalog_storage_date TIMESTAMP,
    custom jsonb
);

CREATE TABLE dsib
(
    parent_id bigint NOT NULL REFERENCES external_input(id) ON DELETE CASCADE,
    PRIMARY KEY (parent_id)
);

CREATE TABLE chunk
(
    parent_id bigint NOT NULL REFERENCES external_input(id) ON DELETE CASCADE,
    dsib_id bigint NOT NULL REFERENCES dsib(parent_id) ON UPDATE CASCADE ON DELETE CASCADE,
    PRIMARY KEY (parent_id)
);

--ALTER TABLE chunk ADD CONSTRAINT dsib_chunk_constraint FOREIGN KEY (dsib_id) REFERENCES dsib(parent_id);

CREATE TABLE aux_data
(
    parent_id bigint NOT NULL REFERENCES external_input(id) ON DELETE CASCADE,
    PRIMARY KEY (parent_id)
);

CREATE TABLE product
(
    id bigserial PRIMARY KEY,
    filename VARCHAR NOT NULL UNIQUE,
    custom jsonb,
    timeliness_name VARCHAR,
    timeliness_value_seconds INTEGER,
    end_to_end_product boolean,
    duplicate boolean,
    t0_pdgs_date TIMESTAMP,
    prip_storage_date TIMESTAMP,
    late boolean
);
