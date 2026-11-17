-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `orders` (`id` INT, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `sales` INT, `store_id` INT, `order_date` DATE, `customer_id` INT);
CREATE TABLE `stores` (`store_id` INT, `store_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
-- 参考解；等价解须从同一初始快照另行运行。
WITH a AS (SELECT store_id,SUM(sales) AS jan FROM orders WHERE SUBSTR(order_date,1,7)='2026-01' GROUP BY store_id),b AS (SELECT store_id,SUM(sales) AS feb FROM orders WHERE SUBSTR(order_date,1,7)='2026-02' GROUP BY store_id) SELECT s.store_id,COALESCE(b.feb,0)-COALESCE(a.jan,0) AS delta FROM stores s LEFT JOIN a ON a.store_id=s.store_id LEFT JOIN b ON b.store_id=s.store_id ORDER BY s.store_id;
