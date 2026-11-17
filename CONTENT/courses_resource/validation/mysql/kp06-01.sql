-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `nullable_values` (`id` INT, `value` INT, `note` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `nullable_values` VALUES (1,NULL,NULL),
(2,0,''),
(3,10,'ok'),
(4,10,'ok');

SELECT COUNT(*) AS rows_count, COUNT(value) AS nonnull_count, COUNT(DISTINCT value) AS unique_count FROM nullable_values;
