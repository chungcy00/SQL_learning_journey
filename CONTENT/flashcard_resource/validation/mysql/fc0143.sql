-- fc0143：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `store_products` (`store_id` INT, `product_id` INT, `net_cents` INT) ENGINE=InnoDB;
INSERT INTO `store_products` VALUES (1, 1, 800), (1, 2, 800), (1, 3, 600), (1, 4, 300), (2, 1, 100), (2, 2, 200);
WITH r AS (SELECT store_id,product_id,ROW_NUMBER() OVER(PARTITION BY store_id ORDER BY net_cents DESC,product_id) AS rn FROM store_products) SELECT store_id,product_id FROM r WHERE rn<=3 ORDER BY store_id,rn;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["store_id", "product_id"], "rows": [[1, 1], [1, 2], [1, 3], [2, 2], [2, 1]]}
