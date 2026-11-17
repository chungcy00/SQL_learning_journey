-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `purchase_items` (`po_item_id` INT, `po_id` INT, `product_id` INT, `quantity_ordered` INT, `unit_price` DECIMAL(12,2)) ENGINE=InnoDB;

INSERT INTO `purchase_items` VALUES (1,1,1,10,8),
(2,1,2,5,20);

CREATE TABLE `receipts` (`receipt_id` INT, `po_item_id` INT, `receipt_date` DATE, `quantity_received` INT) ENGINE=InnoDB;

INSERT INTO `receipts` VALUES (1,1,'2026-01-10',4),
(2,1,'2026-01-12',5);

WITH r AS (SELECT po_item_id,SUM(quantity_received) AS received FROM receipts WHERE receipt_date<'2026-02-01' GROUP BY po_item_id) SELECT i.po_item_id,i.quantity_ordered-COALESCE(r.received,0) AS remaining FROM purchase_items i LEFT JOIN r ON i.po_item_id=r.po_item_id ORDER BY i.po_item_id;
