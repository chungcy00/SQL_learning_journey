-- 同商品跨供应商采购价差
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
WITH a AS (SELECT product_id,supplier_id,SUM(ordered_cents)*1.0/SUM(quantity_ordered) avg_price FROM v_po_lines WHERE order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY product_id,supplier_id), b AS (SELECT *,MIN(avg_price) OVER(PARTITION BY product_id) low,COUNT(*) OVER(PARTITION BY product_id) n FROM a) SELECT product_id,supplier_id,ROUND(avg_price,2) weighted_price_cents,ROUND(low,2) low_price_cents,ROUND(100.0*(avg_price-low)/NULLIF(low,0),2) premium_pct FROM b WHERE n>=2 ORDER BY product_id,premium_pct,supplier_id;

-- 等价解（SQLite已验证，MySQL待复验）
WITH a AS (SELECT product_id,supplier_id,SUM(ordered_cents)*1.0/SUM(quantity_ordered) avg_price FROM v_po_lines WHERE (order_date>='2025-01-01' AND order_date<='2025-12-31') GROUP BY product_id,supplier_id), b AS (SELECT *,MIN(avg_price) OVER(PARTITION BY product_id) low,COUNT(*) OVER(PARTITION BY product_id) n FROM a) SELECT product_id,supplier_id,ROUND(avg_price,2) weighted_price_cents,ROUND(low,2) low_price_cents,ROUND(100.0*(avg_price-low)/NULLIF(low,0),2) premium_pct FROM b WHERE n>=2 ORDER BY product_id,premium_pct,supplier_id;
