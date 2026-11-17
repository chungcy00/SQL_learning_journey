-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `commerce_orders` (`order_id` INT, `customer_id` INT, `order_date` DATE, `status` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
INSERT INTO `commerce_orders` VALUES
(11,1,'2026-01-01','completed'),
(12,2,'2026-02-01','cancelled'),
(13,5,'2026-03-01','completed'),
(14,1,'2026-03-01','completed');
CREATE TABLE `customers` (`customer_id` INT, `customer_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
INSERT INTO `customers` VALUES
(1,'甲'),
(2,'乙'),
(3,'丙'),
(4,'丁'),
(5,'戊'),
(6,'己'),
(7,'庚');
-- 参考解；等价解须从同一初始快照另行运行。
SELECT customer_id,customer_name FROM customers WHERE customer_id IN (SELECT customer_id FROM commerce_orders WHERE status='completed') ORDER BY customer_id;
