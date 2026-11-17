-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `commerce_orders` (`order_id` INT, `customer_id` INT, `order_date` DATE, `status` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
CREATE TABLE `customers` (`customer_id` INT, `customer_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT customer_id,customer_name FROM customers WHERE customer_id IN (SELECT customer_id FROM commerce_orders WHERE status='completed') ORDER BY customer_id;
