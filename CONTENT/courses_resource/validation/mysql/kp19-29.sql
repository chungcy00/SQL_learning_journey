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

WITH running AS (SELECT r.po_item_id,r.receipt_date,r.receipt_id,SUM(r.quantity_received) OVER(PARTITION BY r.po_item_id ORDER BY r.receipt_date,r.receipt_id ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cum_qty FROM receipts r WHERE r.receipt_date<='2026-01-31'), done AS (SELECT i.po_item_id,MIN(CASE WHEN r.cum_qty>=i.quantity_ordered THEN r.receipt_date END) AS completed_date FROM purchase_items i LEFT JOIN running r ON i.po_item_id=r.po_item_id GROUP BY i.po_item_id) SELECT i.po_item_id,d.completed_date,GREATEST(DATEDIFF(COALESCE(d.completed_date,'2026-01-31'),o.promised_date),0) AS delay_days FROM purchase_items i JOIN purchase_orders o ON i.po_id=o.po_id JOIN done d ON i.po_item_id=d.po_item_id WHERE o.status='approved' ORDER BY i.po_item_id;
