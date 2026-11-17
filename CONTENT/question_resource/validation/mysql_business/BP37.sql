-- 供应商延迟与仓库资金压力
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
WITH a AS (SELECT supplier_id,warehouse_id,product_id,SUM((quantity_ordered-accepted)*unit_price_cents) overdue_value FROM v_po_lines WHERE promised_date BETWEEN '2025-01-01' AND '2025-12-31' AND quantity_ordered>accepted GROUP BY supplier_id,warehouse_id,product_id) SELECT a.supplier_id,COUNT(*) affected_pairs,SUM(a.overdue_value) overdue_cents,SUM(i.quantity*i.unit_cost_cents) exposed_stock_cents FROM a JOIN inventory_snapshots i ON i.warehouse_id=a.warehouse_id AND i.product_id=a.product_id AND i.snapshot_date='2025-12-31' GROUP BY a.supplier_id ORDER BY overdue_cents DESC,a.supplier_id;

-- 等价解（SQLite已验证，MySQL待复验）
WITH a AS (SELECT supplier_id,warehouse_id,product_id,SUM((quantity_ordered-accepted)*unit_price_cents) overdue_value FROM v_po_lines WHERE (promised_date>='2025-01-01' AND promised_date<='2025-12-31') AND quantity_ordered>accepted GROUP BY supplier_id,warehouse_id,product_id) SELECT a.supplier_id,COUNT(*) affected_pairs,SUM(a.overdue_value) overdue_cents,SUM(i.quantity*i.unit_cost_cents) exposed_stock_cents FROM a JOIN inventory_snapshots i ON i.warehouse_id=a.warehouse_id AND i.product_id=a.product_id AND i.snapshot_date='2025-12-31' GROUP BY a.supplier_id ORDER BY overdue_cents DESC,a.supplier_id;
