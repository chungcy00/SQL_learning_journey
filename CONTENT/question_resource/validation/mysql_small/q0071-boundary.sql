-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `order_items` (`order_item_id` INT, `order_id` INT, `product_id` INT, `quantity` INT, `unit_price` DECIMAL(12,2), `discount_amount` DECIMAL(12,2));
INSERT INTO `order_items` VALUES
(20,11,11,2,100,10),
(21,11,12,1,25,5),
(22,12,12,3,60,3),
(23,13,14,1,60,0),
(24,15,15,2,40,0);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT SUM(quantity*unit_price-discount_amount) AS net_sales FROM order_items;
