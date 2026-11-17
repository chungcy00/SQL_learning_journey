-- fc0117：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `events` (`id` INT, `day` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `value` INT) ENGINE=InnoDB;
INSERT INTO `events` VALUES (1, '2025-01-01', 10), (2, '2025-01-01', 20), (3, '2025-01-02', 30), (4, '2025-01-03', 40);
SELECT id,SUM(value) OVER(ORDER BY day RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS total FROM events ORDER BY id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id", "total"], "rows": [[1, 30], [2, 30], [3, 60], [4, 100]]}
