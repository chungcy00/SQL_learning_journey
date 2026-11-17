-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `employees` (`employee_id` INT, `employee_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `manager_id` INT, `salary` INT);
INSERT INTO `employees` VALUES
(1,'总经理',NULL,7000),
(2,'主管',1,5000),
(3,'专员',1,5000),
(4,'顾问',2,8000),
(5,'新人',2,3000),
(6,'未知薪资',4,NULL),
(7,'另一个根',NULL,5000);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT employee_id,salary*12 AS annual_salary FROM employees ORDER BY employee_id;
