-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `hr_employees` (`employee_id` INT, `department_id` INT, `employee_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `hire_date` DATE, `termination_date` DATE) ENGINE=InnoDB;

INSERT INTO `hr_employees` VALUES (1,1,'A','2025-01-01',NULL),
(2,1,'B','2025-02-01','2026-02-01'),
(3,2,'C','2026-02-02',NULL);

CREATE TABLE `salaries` (`salary_id` INT, `employee_id` INT, `salary_month` DATE, `base_salary` DECIMAL(12,2), `bonus` DECIMAL(12,2)) ENGINE=InnoDB;

INSERT INTO `salaries` VALUES (1,1,'2026-01-01',4000,500),
(2,2,'2026-01-01',3000,0),
(3,3,'2026-02-01',2500,250);

SELECT e.department_id,e.employee_id,s.base_salary+s.bonus AS total_pay,RANK() OVER(PARTITION BY e.department_id ORDER BY s.base_salary+s.bonus DESC) AS rnk FROM hr_employees e JOIN salaries s ON e.employee_id=s.employee_id WHERE s.salary_month='2026-01-01' ORDER BY e.department_id,rnk,e.employee_id;
