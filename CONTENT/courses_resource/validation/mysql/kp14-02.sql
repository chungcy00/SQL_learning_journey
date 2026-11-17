-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `employees` (`employee_id` INT, `employee_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `manager_id` INT, `salary` INT) ENGINE=InnoDB;

INSERT INTO `employees` VALUES (1,'A',NULL,8000),
(2,'B',1,5000),
(3,'C',1,5000),
(4,'D',2,3000);

WITH RECURSIVE tree AS (SELECT employee_id,employee_name,0 AS depth FROM employees WHERE manager_id IS NULL UNION ALL SELECT e.employee_id,e.employee_name,t.depth+1 FROM employees e JOIN tree t ON e.manager_id=t.employee_id WHERE t.depth<5) SELECT * FROM tree ORDER BY depth,employee_id;
