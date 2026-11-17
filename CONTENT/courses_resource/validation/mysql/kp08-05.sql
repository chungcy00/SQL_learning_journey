-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `customers` (`customer_id` INT, `customer_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `customers` VALUES (1,'林'),
(2,'陈'),
(3,'王'),
(4,'李');

CREATE TABLE `orders` (`id` INT, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `sales` INT, `store_id` INT, `order_date` DATE, `customer_id` INT) ENGINE=InnoDB;

INSERT INTO `orders` VALUES (1,'护肤品',800,1,'2026-01-03',1),
(2,'化妆品',400,2,'2026-01-10',2),
(3,'护肤品',600,1,'2026-02-02',1),
(4,'化妆品',300,2,'2026-02-15',NULL),
(5,'日用品',1500,1,'2026-02-20',3);

WITH totals AS (SELECT customer_id,SUM(sales) AS total FROM orders WHERE customer_id IS NOT NULL GROUP BY customer_id) SELECT c.customer_id,COALESCE(t.total,0) AS total FROM customers c LEFT JOIN totals t ON c.customer_id=t.customer_id ORDER BY c.customer_id;
