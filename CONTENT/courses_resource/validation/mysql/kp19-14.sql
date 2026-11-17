-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `stock_snapshots` (`snapshot_id` INT, `product_id` INT, `warehouse_id` INT, `snapshot_date` DATE, `quantity` INT) ENGINE=InnoDB;

INSERT INTO `stock_snapshots` VALUES (1,1,1,'2026-01-01',100),
(2,1,1,'2026-01-31',60),
(3,2,1,'2026-01-31',20);

CREATE TABLE `inventory_products` (`product_id` INT, `product_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `unit_cost` DECIMAL(12,2), `reorder_level` INT) ENGINE=InnoDB;

INSERT INTO `inventory_products` VALUES (1,'Mask','护肤品',10,80),
(2,'Soap','日用品',5,10);

SELECT s.warehouse_id,SUM(s.quantity*p.unit_cost) AS stock_value FROM stock_snapshots s JOIN inventory_products p ON s.product_id=p.product_id WHERE s.snapshot_date='2026-01-31' GROUP BY s.warehouse_id ORDER BY s.warehouse_id;
