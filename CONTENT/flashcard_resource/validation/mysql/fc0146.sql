-- fc0146：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `category_sales` (`category_id` INT, `net_cents` INT) ENGINE=InnoDB;
INSERT INTO `category_sales` VALUES (1, 300), (2, 100), (3, 100);
SELECT category_id,100.0*net_cents/NULLIF(SUM(net_cents) OVER(),0) AS pct FROM category_sales ORDER BY category_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["category_id", "pct"], "rows": [[1, 60.0], [2, 20.0], [3, 20.0]]}
