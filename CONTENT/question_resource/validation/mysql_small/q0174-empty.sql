-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `purchase_items` (`po_item_id` INT, `po_id` INT, `product_id` INT, `quantity_ordered` INT, `unit_price` DECIMAL(12,2));
CREATE TABLE `receipts` (`receipt_id` INT, `po_item_id` INT, `receipt_date` DATE, `quantity_received` INT);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT r.receipt_id,ROUND(1.0*r.quantity_received/NULLIF(i.quantity_ordered,0),2) AS receipt_ratio FROM receipts r JOIN purchase_items i ON i.po_item_id=r.po_item_id ORDER BY r.receipt_id;
