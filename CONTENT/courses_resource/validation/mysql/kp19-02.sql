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

WITH s AS (SELECT SUM(CASE WHEN snapshot_date='2026-01-01' THEN quantity*10 ELSE 0 END) AS opening_value,SUM(CASE WHEN snapshot_date='2026-01-31' THEN quantity*10 ELSE 0 END) AS closing_value FROM stock_snapshots WHERE product_id=1 AND warehouse_id=1), m AS (SELECT SUM(quantity*unit_cost) AS sales_cost FROM stock_movements WHERE product_id=1 AND warehouse_id=1 AND movement_type='sale_out' AND movement_date>='2026-01-01' AND movement_date<'2026-02-01') SELECT ROUND(2.0*sales_cost/NULLIF(opening_value+closing_value,0),2) AS turnover FROM s CROSS JOIN m;
