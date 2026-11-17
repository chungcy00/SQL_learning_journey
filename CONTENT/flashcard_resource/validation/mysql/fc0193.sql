-- fc0193：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `supplier_risk` (`supplier_id` INT, `overdue_cents` INT, `unpaid_cents` INT, `on_time_pct` INT, `due_lines` INT) ENGINE=InnoDB;
INSERT INTO `supplier_risk` VALUES (1, 600000, 1000000, 80, 20), (2, 100000, 1000000, 50, 12), (3, 100000, 1000000, 50, 8), (4, 500000, 2000000, 80, 20);
SELECT supplier_id FROM supplier_risk WHERE (overdue_cents>=500000 AND 100.0*overdue_cents/NULLIF(unpaid_cents,0)>40) OR (on_time_pct<60 AND due_lines>=10) ORDER BY supplier_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["supplier_id"], "rows": [[1], [2]]}
