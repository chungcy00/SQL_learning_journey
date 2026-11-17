-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `purchase_items` (`po_item_id` INT, `po_id` INT, `product_id` INT, `quantity_ordered` INT, `unit_price` DECIMAL(12,2));
INSERT INTO `purchase_items` VALUES
(21,1,11,12,10),
(22,1,12,5,20),
(23,2,13,0,30),
(24,3,15,11,10),
(25,3,18,8,5);
CREATE TABLE `receipts` (`receipt_id` INT, `po_item_id` INT, `receipt_date` DATE, `quantity_received` INT);
INSERT INTO `receipts` VALUES
(11,21,'2026-01-11',4),
(12,21,'2026-01-12',5),
(13,23,'2026-01-31',0),
(14,24,'2026-02-09',6);
-- 参考解；等价解须从同一初始快照另行运行。
WITH r AS (SELECT po_item_id,SUM(quantity_received) AS received FROM receipts GROUP BY po_item_id) SELECT i.po_item_id,i.quantity_ordered,COALESCE(r.received,0) AS received FROM purchase_items i LEFT JOIN r ON i.po_item_id=r.po_item_id ORDER BY i.po_item_id;
