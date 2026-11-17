-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `old_customers` (`customer_id` INT) ENGINE=InnoDB;

INSERT INTO `old_customers` VALUES (1),
(2),
(2),
(3);

CREATE TABLE `new_customers` (`customer_id` INT) ENGINE=InnoDB;

INSERT INTO `new_customers` VALUES (2),
(3),
(4);

SELECT customer_id FROM old_customers INTERSECT SELECT customer_id FROM new_customers ORDER BY customer_id;
