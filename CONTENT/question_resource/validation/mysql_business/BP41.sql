-- 采购承诺、收货、开票及付款敞口
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
WITH a AS (SELECT supplier_id,SUM(ordered_cents) ordered,SUM(accepted*unit_price_cents) received FROM v_po_lines GROUP BY supplier_id), b AS (SELECT supplier_id,SUM(billed_cents) billed,SUM(paid_cents) paid,SUM(balance_cents) balance FROM v_invoice_balance GROUP BY supplier_id) SELECT s.supplier_id,COALESCE(a.ordered,0) ordered_cents,COALESCE(a.received,0) received_cents,COALESCE(b.billed,0) billed_cents,COALESCE(b.paid,0) paid_cents,COALESCE(b.balance,0) balance_cents FROM suppliers s LEFT JOIN a ON a.supplier_id=s.supplier_id LEFT JOIN b ON b.supplier_id=s.supplier_id ORDER BY balance_cents DESC,s.supplier_id;

-- 等价解（SQLite已验证，MySQL待复验）
WITH a AS (SELECT supplier_id,SUM(ordered_cents) ordered,SUM(accepted*unit_price_cents) received FROM v_po_lines GROUP BY supplier_id), b AS (SELECT supplier_id,SUM(billed_cents) billed,SUM(paid_cents) paid,SUM(balance_cents) balance FROM v_invoice_balance GROUP BY supplier_id) SELECT s.supplier_id,COALESCE(a.ordered,0) ordered_cents,COALESCE(a.received,0) received_cents,COALESCE(b.billed,0) billed_cents,COALESCE(b.paid,0) paid_cents,COALESCE(b.balance,0) balance_cents FROM suppliers s LEFT OUTER JOIN a ON a.supplier_id=s.supplier_id LEFT JOIN b ON b.supplier_id=s.supplier_id ORDER BY balance_cents DESC,s.supplier_id;
