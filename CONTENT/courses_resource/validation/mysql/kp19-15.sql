-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `commerce_orders` (`order_id` INT, `customer_id` INT, `order_date` DATE, `status` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `commerce_orders` VALUES (1,1,'2026-01-01','completed'),
(2,1,'2026-02-01','completed'),
(3,2,'2026-02-01','completed'),
(4,3,'2026-02-02','cancelled');

CREATE TABLE `commerce_items` (`order_item_id` INT, `order_id` INT, `product_id` INT, `quantity` INT, `unit_price` DECIMAL(12,2), `discount_amount` DECIMAL(12,2)) ENGINE=InnoDB;

INSERT INTO `commerce_items` VALUES (1,1,1,1,100,0),
(2,2,1,2,100,10),
(3,3,2,1,50,0),
(4,4,2,1,50,0);

WITH a AS (SELECT o.order_id,SUM(i.quantity*i.unit_price-i.discount_amount) AS order_total FROM commerce_orders o JOIN commerce_items i ON o.order_id=i.order_id WHERE o.status='completed' GROUP BY o.order_id) SELECT COUNT(*) AS valid_orders,ROUND(1.0*SUM(order_total)/NULLIF(COUNT(*),0),2) AS avg_order_value FROM a;
