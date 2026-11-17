-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `employees` (`employee_id` INT, `employee_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `manager_id` INT, `salary` INT) ENGINE=InnoDB;

INSERT INTO `employees` VALUES (1,'A',NULL,8000),
(2,'B',1,5000),
(3,'C',1,5000),
(4,'D',2,3000);

SELECT employee_id,salary,SUM(salary) OVER(ORDER BY salary RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS range_sum,SUM(salary) OVER(ORDER BY salary,employee_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS row_sum FROM employees ORDER BY salary,employee_id;
