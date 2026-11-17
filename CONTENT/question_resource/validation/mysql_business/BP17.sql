-- 供应商发票与付款核对
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
SELECT supplier_invoice_id,supplier_id,billed_cents,paid_cents,balance_cents,CASE WHEN paid_cents=0 THEN 'unpaid' WHEN balance_cents>0 THEN 'partial' WHEN balance_cents=0 THEN 'paid' ELSE 'overpaid' END payment_state FROM v_invoice_balance ORDER BY balance_cents DESC,supplier_invoice_id;

-- 等价解（SQLite已验证，MySQL待复验）
SELECT supplier_invoice_id,supplier_id,billed_cents,paid_cents,balance_cents,CASE WHEN NOT (paid_cents<>0) THEN 'unpaid' WHEN balance_cents>0 THEN 'partial' WHEN balance_cents=0 THEN 'paid' ELSE 'overpaid' END payment_state FROM v_invoice_balance ORDER BY balance_cents DESC,supplier_invoice_id;
