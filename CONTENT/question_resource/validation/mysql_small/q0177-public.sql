-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `order_items` (`order_item_id` INT, `order_id` INT, `product_id` INT, `quantity` INT, `unit_price` DECIMAL(12,2), `discount_amount` DECIMAL(12,2));
INSERT INTO `order_items` VALUES
(1,1,1,2,100,10),
(2,2,1,1,100,0),
(3,3,2,3,50,0);
CREATE TABLE `purchase_items` (`po_item_id` INT, `po_id` INT, `product_id` INT, `quantity_ordered` INT, `unit_price` DECIMAL(12,2));
INSERT INTO `purchase_items` VALUES
(1,1,1,10,8),
(2,1,2,5,20);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT product_id FROM order_items INTERSECT SELECT product_id FROM purchase_items ORDER BY product_id;
