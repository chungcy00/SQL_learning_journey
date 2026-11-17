-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `purchase_items` (`po_item_id` INT, `po_id` INT, `product_id` INT, `quantity_ordered` INT, `unit_price` DECIMAL(12,2));
CREATE TABLE `purchase_orders` (`po_id` INT, `supplier_id` INT, `order_date` DATE, `promised_date` DATE, `status` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
CREATE TABLE `receipts` (`receipt_id` INT, `po_item_id` INT, `receipt_date` DATE, `quantity_received` INT);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT r.receipt_id,DATEDIFF(r.receipt_date,p.promised_date) AS delay_days FROM receipts r JOIN purchase_items i ON r.po_item_id=i.po_item_id JOIN purchase_orders p ON p.po_id=i.po_id ORDER BY r.receipt_id;
