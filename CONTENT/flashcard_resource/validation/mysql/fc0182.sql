-- fc0182：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `share_mini` (`product_id` INT, `net_cents` INT, `stock_value_cents` INT) ENGINE=InnoDB;
INSERT INTO `share_mini` VALUES (1, 6000, 1000), (2, 3000, 6000), (3, 1000, 3000);
SELECT product_id,100.0*stock_value_cents/NULLIF(SUM(stock_value_cents) OVER(),0) AS inventory_pct FROM share_mini ORDER BY product_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["product_id", "inventory_pct"], "rows": [[1, 10.0], [2, 60.0], [3, 30.0]]}
