-- fc0176：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `po_mini` (`po_item_id` INT, `quantity_ordered` INT, `unit_price_cents` INT) ENGINE=InnoDB;
INSERT INTO `po_mini` VALUES (1, 10, 100), (2, 4, 200);
CREATE TABLE `delivery_mini` (`receipt_id` INT, `po_item_id` INT, `quantity_received` INT, `quantity_rejected` INT) ENGINE=InnoDB;
INSERT INTO `delivery_mini` VALUES (1, 1, 4, 0), (2, 1, 5, 1), (3, 2, 4, 0);
CREATE TABLE `bill_mini` (`bill_id` INT, `po_item_id` INT, `quantity_billed` INT, `unit_price_cents` INT) ENGINE=InnoDB;
INSERT INTO `bill_mini` VALUES (1, 1, 3, 100), (2, 1, 5, 110), (3, 2, 4, 200);
WITH r AS (SELECT po_item_id,SUM(quantity_received-quantity_rejected) AS accepted FROM delivery_mini GROUP BY po_item_id),b AS (SELECT po_item_id,SUM(quantity_billed) AS billed FROM bill_mini GROUP BY po_item_id) SELECT p.po_item_id,COALESCE(r.accepted,0) AS accepted,COALESCE(b.billed,0) AS billed FROM po_mini p LEFT JOIN r ON r.po_item_id=p.po_item_id LEFT JOIN b ON b.po_item_id=p.po_item_id ORDER BY p.po_item_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["po_item_id", "accepted", "billed"], "rows": [[1, 8, 8], [2, 4, 4]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
