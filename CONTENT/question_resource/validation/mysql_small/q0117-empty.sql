-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `customers` (`customer_id` INT, `customer_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
CREATE TABLE `orders` (`id` INT, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `sales` INT, `store_id` INT, `order_date` DATE, `customer_id` INT);
-- 参考解；等价解须从同一初始快照另行运行。
WITH a AS (SELECT customer_id,SUM(sales) AS total,COUNT(*) AS n FROM orders GROUP BY customer_id) SELECT c.customer_id,COALESCE(a.total,0) AS total,COALESCE(a.n,0) AS n FROM customers c LEFT JOIN a ON c.customer_id=a.customer_id ORDER BY c.customer_id;
