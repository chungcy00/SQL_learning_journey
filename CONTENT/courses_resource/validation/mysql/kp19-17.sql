-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `commerce_orders` (`order_id` INT, `customer_id` INT, `order_date` DATE, `status` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `commerce_orders` VALUES (1,1,'2026-01-01','completed'),
(2,1,'2026-02-01','completed'),
(3,2,'2026-02-01','completed'),
(4,3,'2026-02-02','cancelled');

WITH first_buy AS (SELECT customer_id,MIN(order_date) AS first_day FROM commerce_orders WHERE status='completed' GROUP BY customer_id), jan AS (SELECT customer_id FROM first_buy WHERE first_day>='2026-01-01' AND first_day<'2026-02-01') SELECT COUNT(*) AS cohort_size,SUM(CASE WHEN EXISTS (SELECT 1 FROM commerce_orders o WHERE o.customer_id=j.customer_id AND o.status='completed' AND o.order_date>='2026-02-01' AND o.order_date<'2026-03-01') THEN 1 ELSE 0 END) AS retained,ROUND(100.0*SUM(CASE WHEN EXISTS (SELECT 1 FROM commerce_orders o WHERE o.customer_id=j.customer_id AND o.status='completed' AND o.order_date>='2026-02-01' AND o.order_date<'2026-03-01') THEN 1 ELSE 0 END)/NULLIF(COUNT(*),0),2) AS retention_pct FROM jan j;
