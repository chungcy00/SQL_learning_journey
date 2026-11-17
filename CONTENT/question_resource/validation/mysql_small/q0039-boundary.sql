-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
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
SELECT customer_id,customer_name FROM customers WHERE customer_id IN (1,3,4) ORDER BY customer_id;
