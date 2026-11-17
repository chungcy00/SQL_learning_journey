-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `orders` (`id` INT, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `sales` INT, `store_id` INT, `order_date` DATE, `customer_id` INT);
INSERT INTO `orders` VALUES
(11,'护肤品',300,1,'2026-01-31',1),
(12,'化妆品',800,2,'2026-02-01',2),
(13,'护肤品',700,1,'2026-02-01',1),
(14,'日用品',NULL,3,'2026-03-01',NULL),
(15,'化妆品',400,2,'2026-02-28',2),
(16,'护肤品',1000,1,'2026-01-01',4),
(17,'日用品',1500,3,'2026-02-20',4),
(18,'护肤品',800,1,'2026-02-28',1),
(19,'化妆品',800,2,'2026-02-25',3),
(20,'日用品',NULL,3,'2026-02-10',NULL),
(21,'日用品',100,4,'2026-02-01',NULL),
(22,'护肤品',0,2,'2026-01-01',5),
(23,'护肤品',100,2,'2026-01-10',5);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT store_id,order_date,id FROM orders ORDER BY store_id,order_date DESC,id;
