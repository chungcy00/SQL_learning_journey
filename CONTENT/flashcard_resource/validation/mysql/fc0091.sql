-- fc0091：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `date_demo` (`id` INT, `day` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `ts` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;
INSERT INTO `date_demo` VALUES (1, '2025-01-31', '2025-01-31 23:59:59'), (2, '2025-02-01', '2025-02-01 00:00:00'), (3, '2025-02-28', '2025-02-28 23:59:59'), (4, '2025-03-01', '2025-03-01 00:00:00');
SELECT id,DATE_FORMAT(day,'%Y-%m') AS ym FROM date_demo ORDER BY id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id", "ym"], "rows": [[1, "2025-01"], [2, "2025-02"], [3, "2025-02"], [4, "2025-03"]]}
