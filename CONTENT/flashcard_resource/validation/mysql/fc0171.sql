-- fc0171：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `salary_mini` (`employee_id` INT, `department_id` INT, `position_id` INT, `base_cents` INT) ENGINE=InnoDB;
INSERT INTO `salary_mini` VALUES (1, 1, 1, 100000), (2, 1, 1, 120000), (3, 1, 1, 140000), (4, 1, 2, 300000);
SELECT employee_id,AVG(base_cents) OVER(PARTITION BY department_id,position_id) AS group_mean FROM salary_mini WHERE department_id=1 AND position_id=1 ORDER BY employee_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["employee_id", "group_mean"], "rows": [[1, 120000.0], [2, 120000.0], [3, 120000.0]]}
