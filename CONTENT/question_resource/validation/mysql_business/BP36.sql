-- 收货入库完整性核对
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
WITH a AS (SELECT receipt_id,SUM(quantity_delta) qty FROM stock_movements WHERE movement_type='receipt' GROUP BY receipt_id) SELECT g.receipt_id,g.po_item_id,g.quantity_received-g.quantity_rejected expected_units,COALESCE(a.qty,0) posted_units,COALESCE(a.qty,0)-(g.quantity_received-g.quantity_rejected) difference FROM goods_receipts g LEFT JOIN a ON a.receipt_id=g.receipt_id WHERE COALESCE(a.qty,0)<>g.quantity_received-g.quantity_rejected ORDER BY g.receipt_id;

-- 等价解（SQLite已验证，MySQL待复验）
WITH a AS (SELECT receipt_id,SUM(quantity_delta) qty FROM stock_movements WHERE NOT (movement_type<>'receipt') GROUP BY receipt_id) SELECT g.receipt_id,g.po_item_id,g.quantity_received-g.quantity_rejected expected_units,COALESCE(a.qty,0) posted_units,COALESCE(a.qty,0)-(g.quantity_received-g.quantity_rejected) difference FROM goods_receipts g LEFT JOIN a ON a.receipt_id=g.receipt_id WHERE COALESCE(a.qty,0)<>g.quantity_received-g.quantity_rejected ORDER BY g.receipt_id;
