-- 供应商准时、完整交付与质量
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
SELECT supplier_id,COUNT(*) due_lines,ROUND(100.0*SUM(CASE WHEN on_time>=quantity_ordered THEN 1 ELSE 0 END)/COUNT(*),2) ontime_pct,ROUND(100.0*SUM(CASE WHEN accepted>=quantity_ordered THEN 1 ELSE 0 END)/COUNT(*),2) complete_pct,ROUND(100.0*SUM(accepted)/NULLIF(SUM(received),0),2) quality_pct FROM v_po_lines WHERE promised_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY supplier_id ORDER BY supplier_id;

-- 等价解（SQLite已验证，MySQL待复验）
SELECT supplier_id,COUNT(*) due_lines,ROUND(100.0*SUM(CASE WHEN on_time>=quantity_ordered THEN 1 ELSE 0 END)/COUNT(*),2) ontime_pct,ROUND(100.0*SUM(CASE WHEN accepted>=quantity_ordered THEN 1 ELSE 0 END)/COUNT(*),2) complete_pct,ROUND(100.0*SUM(accepted)/NULLIF(SUM(received),0),2) quality_pct FROM v_po_lines WHERE (promised_date>='2025-01-01' AND promised_date<='2025-12-31') GROUP BY supplier_id ORDER BY supplier_id;
