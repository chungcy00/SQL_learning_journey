-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `raw_data` (`id` INT, `name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `amount` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
INSERT INTO `raw_data` VALUES
(11,'  Mask A  ','15',' SKIN '),
(12,'Mask A','15','skin'),
(13,'古晋',NULL,'Skin'),
(14,'S%','-5','HOME'),
(15,'  ','0','home'),
(16,NULL,'25',NULL),
(17,'A_B','0','makeup'),
(18,'张 三','100.25',' OTHER '),
(19,'S%','-5','home'),
(20,'','20','home');
-- 参考解；等价解须从同一初始快照另行运行。
SELECT id,LOWER(TRIM(category)) AS category_key,CAST(amount AS DECIMAL(12,2)) AS numeric_amount FROM raw_data ORDER BY id;
