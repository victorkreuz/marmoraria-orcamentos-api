ALTER TABLE item_orcamento ADD COLUMN ordem INTEGER;

UPDATE item_orcamento AS io
SET ordem = sub.rn
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY orcamento_id ORDER BY id) - 1 AS rn
    FROM item_orcamento
) AS sub
WHERE io.id = sub.id;
