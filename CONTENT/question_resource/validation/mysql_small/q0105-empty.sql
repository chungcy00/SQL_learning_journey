-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `hr_employees` (`employee_id` INT, `department_id` INT, `employee_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `hire_date` DATE, `termination_date` DATE);
CREATE TABLE `salaries` (`salary_id` INT, `employee_id` INT, `salary_month` DATE, `base_salary` DECIMAL(12,2), `bonus` DECIMAL(12,2));
-- 参考解；等价解须从同一初始快照另行运行。
SELECT s.salary_id,e.employee_name,s.base_salary FROM salaries s JOIN hr_employees e ON s.employee_id=e.employee_id ORDER BY s.salary_id;
