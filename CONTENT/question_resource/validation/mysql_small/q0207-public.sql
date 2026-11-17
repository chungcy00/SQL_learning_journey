-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `employees` (`employee_id` INT, `employee_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `manager_id` INT, `salary` INT);
INSERT INTO `employees` VALUES
(1,'A',NULL,8000),
(2,'B',1,5000),
(3,'C',1,5000),
(4,'D',2,3000);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT employee_id,salary,RANK() OVER(ORDER BY salary DESC) AS rank_no FROM employees WHERE salary IS NOT NULL ORDER BY rank_no,employee_id;
