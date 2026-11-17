-- fc0160：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `rfm_score` (`customer_id` INT, `last_day` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `frequency` INT, `net_cents` INT) ENGINE=InnoDB;
INSERT INTO `rfm_score` VALUES (1, '2025-12-01', 4, 4000), (2, '2025-12-31', 1, 1000), (3, '2025-12-31', 5, 3000), (4, '2025-11-01', 6, 5000), (5, '2025-10-01', 3, 2000);
SELECT customer_id,6-NTILE(5) OVER(ORDER BY last_day DESC,customer_id) AS r_score FROM rfm_score ORDER BY customer_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["customer_id", "r_score"], "rows": [[1, 3], [2, 5], [3, 4], [4, 2], [5, 1]]}
