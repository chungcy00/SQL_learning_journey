-- fc0197：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `labor_metric` (`store_id` INT, `net_cents` INT, `labor_cents` INT, `worked_minutes` INT, `overtime_minutes` INT) ENGINE=InnoDB;
INSERT INTO `labor_metric` VALUES (1, 60000, 12000, 300, 60), (2, 10000, 1000, 0, 0);
SELECT store_id,60.0*net_cents/NULLIF(worked_minutes+overtime_minutes,0) AS cents_per_hour FROM labor_metric ORDER BY store_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["store_id", "cents_per_hour"], "rows": [[1, 10000.0], [2, null]]}
