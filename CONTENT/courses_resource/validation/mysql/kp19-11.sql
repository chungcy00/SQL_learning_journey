-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `stock_snapshots` (`snapshot_id` INT, `product_id` INT, `warehouse_id` INT, `snapshot_date` DATE, `quantity` INT) ENGINE=InnoDB;

INSERT INTO `stock_snapshots` VALUES (1,1,1,'2026-01-01',100),
(2,1,1,'2026-01-31',60),
(3,2,1,'2026-01-31',20);

SELECT product_id,warehouse_id,quantity FROM stock_snapshots WHERE snapshot_date='2026-01-31' AND quantity>50 ORDER BY product_id,warehouse_id;
