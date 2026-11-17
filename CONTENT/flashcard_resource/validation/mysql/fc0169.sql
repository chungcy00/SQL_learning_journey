-- fc0169：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `job_mini` (`employee_id` INT, `department_id` INT, `start_date` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `end_date` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;
INSERT INTO `job_mini` VALUES (1, 1, '2024-01-01', '2025-12-31'), (1, 2, '2025-12-31', NULL), (2, 1, '2025-01-01', NULL), (3, 2, '2026-01-01', NULL);
SELECT employee_id,department_id FROM job_mini WHERE start_date<='2025-12-31' AND (end_date IS NULL OR end_date > '2025-12-31') ORDER BY employee_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["employee_id", "department_id"], "rows": [[1, 2], [2, 1]]}
