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

CREATE TABLE `suppliers` (`supplier_id` INT, `supplier_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `region` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `suppliers` VALUES (1,'甲供应商','North'),
(2,'乙供应商','South');

SELECT s.supplier_id,SUM(i.quantity_ordered*i.unit_price) AS order_cost FROM purchase_orders o JOIN suppliers s ON o.supplier_id=s.supplier_id JOIN purchase_items i ON o.po_id=i.po_id WHERE o.status='approved' GROUP BY s.supplier_id ORDER BY s.supplier_id;
