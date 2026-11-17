-- fc0122：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `raw_names` (`id` INT, `label` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `ref` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `updated` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;
INSERT INTO `raw_names` VALUES (1, '  tea ', 'A', '2025-01-01'), (2, 'TEA', 'A', '2025-02-01'), (3, 'Rice', 'B', '2025-01-01'), (4, '', 'C', '2025-01-01'), (5, NULL, 'D', '2025-01-01');
SELECT ref,COUNT(*) AS n FROM raw_names GROUP BY ref HAVING COUNT(*)>1 ORDER BY ref;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["ref", "n"], "rows": [["A", 2]]}
