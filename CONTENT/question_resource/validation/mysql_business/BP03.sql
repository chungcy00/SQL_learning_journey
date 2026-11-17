-- 高销售低毛利商品诊断
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
WITH a AS (SELECT product_id,SUM(net_cents) net,SUM(cost_cents) cost,SUM(discount_cents) discount,SUM(refund_cents) refund FROM v_sales_lines WHERE channel='store' AND order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY product_id), b AS (SELECT *,NTILE(4) OVER(ORDER BY net DESC,product_id) bucket FROM a) SELECT product_id,net net_cents,ROUND(100.0*(net-cost)/NULLIF(net,0),2) margin_pct,discount discount_cents,refund refund_cents FROM b WHERE bucket=1 AND 100.0*(net-cost)/NULLIF(net,0)<30 ORDER BY net DESC,product_id;

-- 等价解（SQLite已验证，MySQL待复验）
WITH a AS (SELECT product_id,SUM(net_cents) net,SUM(cost_cents) cost,SUM(discount_cents) discount,SUM(refund_cents) refund FROM v_sales_lines WHERE channel='store' AND (order_date>='2025-01-01' AND order_date<='2025-12-31') GROUP BY product_id), b AS (SELECT *,NTILE(4) OVER(ORDER BY net DESC,product_id) bucket FROM a) SELECT product_id,net net_cents,ROUND(100.0*(net-cost)/NULLIF(net,0),2) margin_pct,discount discount_cents,refund refund_cents FROM b WHERE bucket=1 AND 100.0*(net-cost)/NULLIF(net,0)<30 ORDER BY net DESC,product_id;
