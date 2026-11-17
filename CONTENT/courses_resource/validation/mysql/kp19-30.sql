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

CREATE TABLE `supplier_invoices` (`invoice_id` INT, `po_id` INT, `invoice_date` DATE, `amount` DECIMAL(12,2)) ENGINE=InnoDB;

INSERT INTO `supplier_invoices` VALUES (1,1,'2026-01-15',100),
(2,1,'2026-01-20',70);

WITH a AS (SELECT po_id,SUM(quantity_ordered*unit_price) AS order_amount FROM purchase_items GROUP BY po_id), b AS (SELECT po_id,SUM(amount) AS invoice_amount FROM supplier_invoices WHERE invoice_date<'2026-02-01' GROUP BY po_id) SELECT o.po_id,a.order_amount,COALESCE(b.invoice_amount,0) AS invoice_amount,a.order_amount-COALESCE(b.invoice_amount,0) AS difference FROM purchase_orders o JOIN a ON o.po_id=a.po_id LEFT JOIN b ON o.po_id=b.po_id WHERE o.status='approved' ORDER BY o.po_id;
