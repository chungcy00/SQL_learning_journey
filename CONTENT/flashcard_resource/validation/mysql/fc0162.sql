-- fc0162：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `channel_totals` (`source_id` INT, `sessions` INT, `converted_sessions` INT, `new_customers` INT, `spend_cents` INT) ENGINE=InnoDB;
INSERT INTO `channel_totals` VALUES (1, 100, 12, 4, 20000), (2, 10, 0, 0, 1000);
SELECT source_id,100.0*converted_sessions/NULLIF(sessions,0) AS conversion_pct FROM channel_totals ORDER BY source_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["source_id", "conversion_pct"], "rows": [[1, 12.0], [2, 0.0]]}
