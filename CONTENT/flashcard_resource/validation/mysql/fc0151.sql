-- fc0151：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `sku_totals` (`product_id` INT, `net_cents` INT, `profit_cents` INT) ENGINE=InnoDB;
INSERT INTO `sku_totals` VALUES (1, 9000, 1800), (2, 9000, 3600), (3, 8000, 2000), (4, 7000, 3500), (5, 6000, 1200), (6, 5000, 2000), (7, 4000, 1000), (8, 3000, 1500);
SELECT product_id,NTILE(4) OVER(ORDER BY net_cents DESC,product_id) AS quartile FROM sku_totals ORDER BY product_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["product_id", "quartile"], "rows": [[1, 1], [2, 1], [3, 2], [4, 2], [5, 3], [6, 3], [7, 4], [8, 4]]}
