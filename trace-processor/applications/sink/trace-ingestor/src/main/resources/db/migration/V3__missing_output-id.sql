ALTER TABLE missing_products DROP CONSTRAINT missing_products_pkey;

ALTER TABLE missing_products
    ADD COLUMN id bigserial PRIMARY KEY;

