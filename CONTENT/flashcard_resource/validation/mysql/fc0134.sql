-- fc0134：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `stock_log` (`id` INT, `qty` INT) ENGINE=InnoDB;
INSERT INTO `stock_log` VALUES (1, 8), (2, 0), (3, 4);
ALTER TABLE stock_log ADD COLUMN status VARCHAR(10) DEFAULT 'active';
SELECT id,status FROM stock_log ORDER BY id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id", "status"], "rows": [[1, "active"], [2, "active"], [3, "active"]]}
