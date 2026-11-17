-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `employees` (`employee_id` INT, `employee_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `manager_id` INT, `salary` INT);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT e.employee_id,m.employee_id AS manager_key FROM employees e LEFT JOIN employees m ON e.manager_id=m.employee_id ORDER BY e.employee_id;
