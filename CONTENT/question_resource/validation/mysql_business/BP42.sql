-- 三方不一致且已经付款的发票
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
WITH a AS (SELECT po_id,SUM(accepted*unit_price_cents) received FROM v_po_lines GROUP BY po_id) SELECT b.supplier_invoice_id,b.po_id,b.billed_cents,b.paid_cents,a.received received_cents,b.billed_cents-a.received bill_difference_cents,b.paid_cents-a.received paid_difference_cents FROM v_invoice_balance b JOIN a ON a.po_id=b.po_id WHERE b.paid_cents>0 AND ABS(b.billed_cents-a.received)>100 ORDER BY ABS(b.billed_cents-a.received) DESC,b.supplier_invoice_id;

-- 等价解（SQLite已验证，MySQL待复验）
WITH a AS (SELECT po_id,SUM(accepted*unit_price_cents) received FROM v_po_lines GROUP BY po_id) SELECT b.supplier_invoice_id,b.po_id,b.billed_cents,b.paid_cents,a.received received_cents,b.billed_cents-a.received bill_difference_cents,b.paid_cents-a.received paid_difference_cents FROM v_invoice_balance b JOIN a ON a.po_id=b.po_id WHERE NOT (b.paid_cents<=0) AND ABS(b.billed_cents-a.received)>100 ORDER BY ABS(b.billed_cents-a.received) DESC,b.supplier_invoice_id;
