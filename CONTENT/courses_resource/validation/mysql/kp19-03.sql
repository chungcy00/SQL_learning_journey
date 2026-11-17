-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `commerce_orders` (`order_id` INT, `customer_id` INT, `order_date` DATE, `status` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `commerce_orders` VALUES (1,1,'2026-01-01','completed'),
(2,1,'2026-02-01','completed'),
(3,2,'2026-02-01','completed'),
(4,3,'2026-02-02','cancelled');

WITH c AS (SELECT customer_id,COUNT(*) AS n FROM commerce_orders WHERE status='completed' AND order_date>='2026-01-01' AND order_date<'2026-03-01' GROUP BY customer_id) SELECT COUNT(*) AS buyers,SUM(CASE WHEN n>=2 THEN 1 ELSE 0 END) AS repeat_buyers,ROUND(100.0*SUM(CASE WHEN n>=2 THEN 1 ELSE 0 END)/NULLIF(COUNT(*),0),2) AS repeat_pct FROM c;
