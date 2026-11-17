-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `orders` (`id` INT, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `sales` INT, `store_id` INT, `order_date` DATE, `customer_id` INT);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT SUM(sales) AS total FROM orders WHERE order_date<'2026-02-01';
