-- fc0060：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `lines` (`id` INT, `order_id` INT, `quantity` INT, `amount` INT) ENGINE=InnoDB;
INSERT INTO `lines` VALUES (1, 1, 2, 30), (2, 1, 1, 10), (3, 2, 3, 60), (4, 3, 1, 30);
CREATE TABLE `receipts` (`id` INT, `order_id` INT, `paid` INT) ENGINE=InnoDB;
INSERT INTO `receipts` VALUES (1, 1, 15), (2, 1, 25), (3, 2, 60);
WITH l AS (SELECT order_id,SUM(amount) AS s FROM lines GROUP BY order_id),p AS (SELECT order_id,SUM(paid) AS p FROM receipts GROUP BY order_id) SELECT l.order_id,l.s,p.p FROM l JOIN p ON l.order_id=p.order_id WHERE l.order_id=1;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["order_id", "s", "p"], "rows": [[1, 40, 40]]}
