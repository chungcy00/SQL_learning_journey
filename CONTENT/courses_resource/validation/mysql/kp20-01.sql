-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `orders` (`id` INT, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `sales` INT, `store_id` INT, `order_date` DATE, `customer_id` INT) ENGINE=InnoDB;

INSERT INTO `orders` VALUES (1,'护肤品',800,1,'2026-01-03',1),
(2,'化妆品',400,2,'2026-01-10',2),
(3,'护肤品',600,1,'2026-02-02',1),
(4,'化妆品',300,2,'2026-02-15',NULL),
(5,'日用品',1500,1,'2026-02-20',3);

CREATE TABLE `stores` (`store_id` INT, `store_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `stores` VALUES (1,'古晋'),
(2,'美里'),
(3,'诗巫');

WITH totals AS (SELECT s.store_id,s.store_name,SUM(o.sales) AS total FROM stores s JOIN orders o ON s.store_id=o.store_id GROUP BY s.store_id,s.store_name) SELECT store_name,total,ROUND(100.0*total/NULLIF(SUM(total) OVER(),0),2) AS share_pct FROM totals ORDER BY total DESC,store_id;
