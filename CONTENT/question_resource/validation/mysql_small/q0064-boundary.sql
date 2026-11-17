-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `nullable_values` (`id` INT, `value` INT, `note` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
INSERT INTO `nullable_values` VALUES
(11,NULL,NULL),
(12,0,''),
(13,10,'ok'),
(14,10,'ok'),
(15,-5,' '),
(16,42,NULL),
(17,NULL,'存在');
-- 参考解；等价解须从同一初始快照另行运行。
SELECT MAX(value) AS max_value FROM nullable_values;
