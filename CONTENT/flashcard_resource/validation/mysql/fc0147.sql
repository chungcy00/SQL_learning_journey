-- fc0147：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `tenure_mini` (`employee_id` INT, `hire_date` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `termination_date` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;
INSERT INTO `tenure_mini` VALUES (1, '2025-12-01', NULL), (2, '2025-12-01', '2025-12-11');
SELECT employee_id,DATEDIFF(COALESCE(termination_date,'2025-12-31'),hire_date) AS days FROM tenure_mini ORDER BY employee_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["employee_id", "days"], "rows": [[1, 30], [2, 10]]}
