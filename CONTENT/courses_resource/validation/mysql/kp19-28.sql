-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `purchase_orders` (`po_id` INT, `supplier_id` INT, `order_date` DATE, `promised_date` DATE, `status` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `purchase_orders` VALUES (1,1,'2026-01-02','2026-01-11','approved'),
(2,2,'2026-01-02','2026-01-12','cancelled');

CREATE TABLE `purchase_items` (`po_item_id` INT, `po_id` INT, `product_id` INT, `quantity_ordered` INT, `unit_price` DECIMAL(12,2)) ENGINE=InnoDB;

INSERT INTO `purchase_items` VALUES (1,1,1,10,8),
(2,1,2,5,20);

CREATE TABLE `receipts` (`receipt_id` INT, `po_item_id` INT, `receipt_date` DATE, `quantity_received` INT) ENGINE=InnoDB;

INSERT INTO `receipts` VALUES (1,1,'2026-01-10',4),
(2,1,'2026-01-12',5);

WITH a AS (SELECT o.supplier_id,i.po_item_id,i.quantity_ordered,COALESCE(SUM(r.quantity_received),0) AS received_on_time FROM purchase_orders o JOIN purchase_items i ON o.po_id=i.po_id LEFT JOIN receipts r ON r.po_item_id=i.po_item_id AND r.receipt_date<=o.promised_date WHERE o.status='approved' AND o.promised_date<='2026-01-31' GROUP BY o.supplier_id,i.po_item_id,i.quantity_ordered) SELECT supplier_id,COUNT(*) AS due_items,SUM(CASE WHEN received_on_time>=quantity_ordered THEN 1 ELSE 0 END) AS on_time_items,ROUND(100.0*SUM(CASE WHEN received_on_time>=quantity_ordered THEN 1 ELSE 0 END)/NULLIF(COUNT(*),0),2) AS fulfillment_pct FROM a GROUP BY supplier_id ORDER BY supplier_id;
