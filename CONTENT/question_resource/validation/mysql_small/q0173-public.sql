-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `hr_employees` (`employee_id` INT, `department_id` INT, `employee_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `hire_date` DATE, `termination_date` DATE);
INSERT INTO `hr_employees` VALUES
(1,1,'A','2025-01-01',NULL),
(2,1,'B','2025-02-01','2026-02-01'),
(3,2,'C','2026-02-02',NULL);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT employee_id FROM hr_employees WHERE hire_date<='2026-02-01' AND (termination_date IS NULL OR termination_date>'2026-02-01') ORDER BY employee_id;
