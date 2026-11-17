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
CREATE TABLE `purchase_items` (`po_item_id` INT, `po_id` INT, `product_id` INT, `quantity_ordered` INT, `unit_price` DECIMAL(12,2));
INSERT INTO `purchase_items` VALUES
(21,1,11,12,10),
(22,1,12,5,20),
(23,2,13,0,30),
(24,3,15,11,10),
(25,3,18,8,5);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT product_id FROM order_items INTERSECT SELECT product_id FROM purchase_items ORDER BY product_id;
