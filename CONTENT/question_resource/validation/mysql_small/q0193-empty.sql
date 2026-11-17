-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `employees` (`employee_id` INT, `employee_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `manager_id` INT, `salary` INT);
-- 参考解；等价解须从同一初始快照另行运行。
WITH RECURSIVE a AS (SELECT employee_id,employee_id AS root_id,0 AS depth FROM employees WHERE manager_id IS NULL UNION ALL SELECT e.employee_id,a.root_id,a.depth+1 FROM employees e JOIN a ON e.manager_id=a.employee_id WHERE a.depth<5) SELECT employee_id,root_id,depth FROM a ORDER BY root_id,depth,employee_id;
