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

CREATE TABLE `retail_products` (`product_id` INT, `product_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `unit_cost` DECIMAL(12,2), `list_price` DECIMAL(12,2)) ENGINE=InnoDB;

INSERT INTO `retail_products` VALUES (1,'Mask','护肤品',60,100),
(2,'Soap','日用品',20,50);

SELECT p.category,SUM(i.quantity*i.unit_price-i.discount_amount) AS net_sales FROM retail_orders o JOIN order_items i ON o.order_id=i.order_id JOIN retail_products p ON i.product_id=p.product_id WHERE o.status='completed' GROUP BY p.category HAVING SUM(i.quantity*i.unit_price-i.discount_amount)>100 ORDER BY net_sales DESC;
