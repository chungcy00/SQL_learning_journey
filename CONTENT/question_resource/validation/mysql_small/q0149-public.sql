-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `order_items` (`order_item_id` INT, `order_id` INT, `product_id` INT, `quantity` INT, `unit_price` DECIMAL(12,2), `discount_amount` DECIMAL(12,2));
INSERT INTO `order_items` VALUES
(1,1,1,2,100,10),
(2,2,1,1,100,0),
(3,3,2,3,50,0);
CREATE TABLE `products` (`product_id` INT, `product_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `price` INT, `stock` INT);
INSERT INTO `products` VALUES
(1,'Mask','护肤品',15,150),
(2,'Serum','护肤品',89,40),
(3,'Cleanser','护肤品',35,200),
(4,'Cream','化妆品',65,180),
(5,'Sunscreen','化妆品',45,80),
(6,'Soap_1','日用品',15,0);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT p.product_id,CASE WHEN EXISTS(SELECT 1 FROM order_items i WHERE i.product_id=p.product_id) THEN 1 ELSE 0 END AS has_item FROM products p ORDER BY p.product_id;
