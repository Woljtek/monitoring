ALTER TABLE product
    DROP COLUMN late;

CREATE OR REPLACE VIEW product_view AS
SELECT p.*,
       (SELECT (EXTRACT(EPOCH FROM (p.prip_storage_end_date - p.t0_pdgs_date)))::int > p.timeliness_value_seconds) AS late,
       EXISTS(SELECT p2.duplicate from output_list ol JOIN processing p2 ON p2.id = ol.processing_id WHERE ol.product_id = p.id AND p2.duplicate) AS duplicate
FROM product p