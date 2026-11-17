-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `products` (`product_id` INT, `product_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `price` INT, `stock` INT) ENGINE=InnoDB;

INSERT INTO `products` VALUES (1,'Mask','护肤品',15,150),
(2,'Serum','护肤品',89,40),
(3,'Cleanser','护肤品',35,200),
(4,'Cream','化妆品',65,180),
(5,'Sunscreen','化妆品',45,80),
(6,'Soap_1','日用品',15,0);

SELECT product_name FROM products WHERE product_name LIKE '%!_%' ESCAPE '!' ORDER BY product_id;
