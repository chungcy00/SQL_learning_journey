-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `raw_data` (`id` INT, `name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `amount` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
INSERT INTO `raw_data` VALUES
(1,'  Mask ','15','SKIN'),
(2,'Mask','15','skin'),
(3,'Serum',NULL,'Skin'),
(4,'Soap','-5','HOME'),
(5,'Cream','0','makeup'),
(6,'','20','HOME');
-- 参考解；等价解须从同一初始快照另行运行。
SELECT id,CAST(amount AS DECIMAL(12,2)) AS numeric_amount FROM raw_data WHERE CAST(amount AS DECIMAL(12,2))<0 ORDER BY id;
