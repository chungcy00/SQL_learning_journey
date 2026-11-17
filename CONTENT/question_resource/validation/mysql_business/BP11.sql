-- 新老客户净收入及客单价
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
WITH a AS (SELECT *,ROW_NUMBER() OVER(PARTITION BY customer_id ORDER BY order_date,order_id) rn FROM v_order_net WHERE channel='online') SELECT CASE WHEN rn=1 THEN 'new' ELSE 'returning' END segment,COUNT(DISTINCT customer_id) customers,COUNT(*) orders,SUM(net_cents) net_cents,ROUND(1.0*SUM(net_cents)/COUNT(*),2) avg_order_cents FROM a WHERE order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY CASE WHEN rn=1 THEN 'new' ELSE 'returning' END ORDER BY segment;

-- 等价解（SQLite已验证，MySQL待复验）
WITH a AS (SELECT *,ROW_NUMBER() OVER(PARTITION BY customer_id ORDER BY order_date,order_id) rn FROM v_order_net WHERE channel='online') SELECT CASE WHEN rn=1 THEN 'new' ELSE 'returning' END segment,COUNT(DISTINCT customer_id) customers,COUNT(*) orders,SUM(net_cents) net_cents,ROUND(1.0*SUM(net_cents)/COUNT(*),2) avg_order_cents FROM a WHERE (order_date>='2025-01-01' AND order_date<='2025-12-31') GROUP BY CASE WHEN rn=1 THEN 'new' ELSE 'returning' END ORDER BY segment;
