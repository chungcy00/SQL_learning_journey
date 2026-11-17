-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `orders` (`id` INT, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `sales` INT, `store_id` INT, `order_date` DATE, `customer_id` INT);
INSERT INTO `orders` VALUES
(1,'护肤品',800,1,'2026-01-03',1),
(2,'化妆品',400,2,'2026-01-10',2),
(3,'护肤品',600,1,'2026-02-02',1),
(4,'化妆品',300,2,'2026-02-15',NULL),
(5,'日用品',1500,1,'2026-02-20',3);
-- 参考解；等价解须从同一初始快照另行运行。
SELECT id,order_date FROM orders WHERE order_date>='2026-02-01' AND order_date<'2026-03-01' ORDER BY order_date,id;
