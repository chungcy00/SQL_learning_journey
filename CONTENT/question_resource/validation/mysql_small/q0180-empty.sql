-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `new_customers` (`customer_id` INT);
CREATE TABLE `old_customers` (`customer_id` INT);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT customer_id FROM old_customers INTERSECT SELECT customer_id FROM new_customers ORDER BY customer_id;
