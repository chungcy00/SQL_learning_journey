-- 应付账款账龄
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
WITH a AS (SELECT v.*,CASE WHEN due_date>='2025-12-31' THEN 'current' WHEN due_date>='2025-12-01' THEN '01-30' WHEN due_date>='2025-11-01' THEN '31-60' WHEN due_date>='2025-10-02' THEN '61-90' ELSE '90+' END bucket FROM v_invoice_balance v WHERE balance_cents>0) SELECT supplier_id,bucket,COUNT(*) invoices,SUM(balance_cents) outstanding_cents FROM a GROUP BY supplier_id,bucket ORDER BY supplier_id,bucket;

-- 等价解（SQLite已验证，MySQL待复验）
WITH a AS (SELECT v.*,CASE WHEN due_date>='2025-12-31' THEN 'current' WHEN due_date>='2025-12-01' THEN '01-30' WHEN due_date>='2025-11-01' THEN '31-60' WHEN due_date>='2025-10-02' THEN '61-90' ELSE '90+' END bucket FROM v_invoice_balance v WHERE balance_cents>0) SELECT supplier_id,bucket,COUNT(1) invoices,SUM(balance_cents) outstanding_cents FROM a GROUP BY supplier_id,bucket ORDER BY supplier_id,bucket;
