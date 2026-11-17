-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `raw_data` (`id` INT, `name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `amount` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `raw_data` VALUES (1,'  Mask ','15','SKIN'),
(2,'Mask','15','skin'),
(3,'Serum',NULL,'Skin'),
(4,'Soap','-5','HOME'),
(5,'Cream','0','makeup'),
(6,'','20','HOME');

SELECT id,CAST(amount AS DECIMAL(12,2)) AS numeric_amount,CASE WHEN LOWER(TRIM(category))='skin' THEN '护肤品' WHEN LOWER(TRIM(category))='home' THEN '日用品' WHEN LOWER(TRIM(category))='makeup' THEN '化妆品' ELSE '待分类' END AS clean_category FROM raw_data ORDER BY id;
