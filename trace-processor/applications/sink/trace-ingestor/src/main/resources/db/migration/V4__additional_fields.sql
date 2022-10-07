ALTER TABLE external_input
    ADD COLUMN catalog_storage_begin_date TIMESTAMP;
ALTER TABLE external_input
    RENAME COLUMN pickup_point_seen_date TO seen_date;
ALTER TABLE external_input
    RENAME COLUMN pickup_point_available_date TO available_date;
ALTER TABLE external_input
    RENAME COLUMN catalog_storage_date TO catalog_storage_end_date;

ALTER TABLE product
    ADD COLUMN generation_begin_date TIMESTAMP,
    ADD COLUMN generation_end_date TIMESTAMP,
    ADD COLUMN catalog_storage_begin_date TIMESTAMP,
    ADD COLUMN catalog_storage_end_date TIMESTAMP,
    ADD COLUMN prip_storage_begin_date TIMESTAMP,
    ADD COLUMN quality_check_date TIMESTAMP,
    ADD COLUMN quality_check_end_date TIMESTAMP,
    ADD COLUMN first_download_date TIMESTAMP,
    ADD COLUMN eviction_date TIMESTAMP;
ALTER TABLE product
    RENAME COLUMN prip_storage_date TO prip_storage_end_date;