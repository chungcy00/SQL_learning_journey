-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `products` (`product_id` INT, `product_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `price` INT, `stock` INT);
INSERT INTO `products` VALUES
(11,'S_ale','护肤品',30,50),
(12,'Serum2','护肤品',45,150),
(13,'Cream2','护肤品',60,180),
(14,'Mask50','化妆品',35,200),
(15,'Sunscreen2','化妆品',60,80),
(16,'Soap_2','日用品',15,0),
(17,'A B','日用品',89,49),
(18,'古晋皂','日用品',15,10),
(19,'S%','护肤品',40,0),
(20,'Zero','日用品',0,100),
(21,'未知','护肤品',NULL,NULL),
(22,'Xer','化妆品',65,80),
(23,'Soda','化妆品',60,181),
(24,'Other','护肤品',15,15),
(25,'Test','化妆品',89,1),
(26,'Aloe','护肤品',50,100),
(27,'Brush','日用品',45,80),
(28,'Axy','特殊',60,0),
(29,'Solo','特殊',61,1),
(30,'TieHigh','化妆品',89,11),
(31,'TieSecond','化妆品',65,12);
CREATE TABLE `purchase_items` (`po_item_id` INT, `po_id` INT, `product_id` INT, `quantity_ordered` INT, `unit_price` DECIMAL(12,2));
INSERT INTO `purchase_items` VALUES
(21,1,11,12,10),
(22,1,12,5,20),
(23,2,13,0,30),
(24,3,15,11,10),
(25,3,18,8,5);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT p.product_id FROM products p WHERE NOT EXISTS(SELECT 1 FROM purchase_items i WHERE i.product_id=p.product_id) ORDER BY p.product_id;
