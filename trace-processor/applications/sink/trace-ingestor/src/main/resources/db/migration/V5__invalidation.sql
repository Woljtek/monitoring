CREATE TYPE responsibility AS ENUM
(
    'PDGS',
    'E2E'
);

CREATE TABLE invalidation
(
    id bigserial PRIMARY KEY,
    responsibility responsibility,
    update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    root_cause VARCHAR,
    anomaly_identifier bigint,
    comment VARCHAR,
    label VARCHAR
);

CREATE TABLE invalidation_timeliness
(
    parent_id bigint NOT NULL REFERENCES invalidation(id) ON DELETE CASCADE,
    product_id bigint NOT NULL REFERENCES product(id) ON UPDATE CASCADE ON DELETE CASCADE,
    PRIMARY KEY (parent_id)
);

CREATE TABLE invalidation_completeness
(
    parent_id bigint NOT NULL REFERENCES invalidation(id) ON DELETE CASCADE,
    missing_products_id bigint NOT NULL REFERENCES missing_products(id) ON UPDATE CASCADE ON DELETE CASCADE,
    PRIMARY KEY (parent_id)
);

CREATE  FUNCTION invalidation_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_date = now();
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER invalidation_update_trigger
    BEFORE UPDATE
    ON
        invalidation
    FOR EACH ROW
EXECUTE PROCEDURE invalidation_update();