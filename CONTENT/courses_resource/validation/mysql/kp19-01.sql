-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `retail_orders` (`order_id` INT, `store_id` INT, `customer_id` INT, `order_date` DATE, `status` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `retail_orders` VALUES (1,1,1,'2026-01-01','completed'),
(2,2,2,'2026-01-02','cancelled'),
(3,1,1,'2026-02-01','completed');

CREATE TABLE `order_items` (`order_item_id` INT, `order_id` INT, `product_id` INT, `quantity` INT, `unit_price` DECIMAL(12,2), `discount_amount` DECIMAL(12,2)) ENGINE=InnoDB;

INSERT INTO `order_items` VALUES (1,1,1,2,100,10),
(2,2,1,1,100,0),
(3,3,2,3,50,0);

CREATE TABLE `stores` (`store_id` INT, `store_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `stores` VALUES (1,'古晋'),
(2,'美里'),
(3,'诗巫');

SELECT s.store_id,COALESCE(SUM(i.quantity*i.unit_price-i.discount_amount),0) AS net_sales FROM stores s LEFT JOIN retail_orders o ON s.store_id=o.store_id AND o.status='completed' LEFT JOIN order_items i ON o.order_id=i.order_id GROUP BY s.store_id ORDER BY s.store_id;
