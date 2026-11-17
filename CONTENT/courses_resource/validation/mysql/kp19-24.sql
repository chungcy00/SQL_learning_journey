-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `hr_employees` (`employee_id` INT, `department_id` INT, `employee_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `hire_date` DATE, `termination_date` DATE) ENGINE=InnoDB;

INSERT INTO `hr_employees` VALUES (1,1,'A','2025-01-01',NULL),
(2,1,'B','2025-02-01','2026-02-01'),
(3,2,'C','2026-02-02',NULL);

WITH a AS (SELECT SUM(CASE WHEN hire_date<='2026-01-31' AND (termination_date IS NULL OR termination_date>'2026-01-31') THEN 1 ELSE 0 END) AS opening,SUM(CASE WHEN hire_date<='2026-02-28' AND (termination_date IS NULL OR termination_date>'2026-02-28') THEN 1 ELSE 0 END) AS closing,SUM(CASE WHEN termination_date>='2026-02-01' AND termination_date<'2026-03-01' THEN 1 ELSE 0 END) AS leavers FROM hr_employees) SELECT opening,closing,leavers,ROUND(200.0*leavers/NULLIF(opening+closing,0),2) AS turnover_pct FROM a;
