-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `stock_snapshots` (`snapshot_id` INT, `product_id` INT, `warehouse_id` INT, `snapshot_date` DATE, `quantity` INT) ENGINE=InnoDB;

INSERT INTO `stock_snapshots` VALUES (1,1,1,'2026-01-01',100),
(2,1,1,'2026-01-31',60),
(3,2,1,'2026-01-31',20);

CREATE TABLE `stock_movements` (`movement_id` INT, `product_id` INT, `warehouse_id` INT, `movement_date` DATE, `movement_type` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `quantity` INT, `unit_cost` DECIMAL(12,2)) ENGINE=InnoDB;

INSERT INTO `stock_movements` VALUES (1,1,1,'2026-01-15','sale_out',40,10);

SELECT s.product_id,s.warehouse_id,s.quantity FROM stock_snapshots s WHERE s.snapshot_date='2026-01-31' AND s.quantity>0 AND NOT EXISTS (SELECT 1 FROM stock_movements m WHERE m.product_id=s.product_id AND m.warehouse_id=s.warehouse_id AND m.movement_type='sale_out' AND m.movement_date>='2026-01-01' AND m.movement_date<'2026-02-01') ORDER BY s.product_id;
