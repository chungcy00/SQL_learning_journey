-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `monthly` (`month` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `sales` INT);
INSERT INTO `monthly` VALUES
('2026-01',1000),
('2026-02',1500),
('2026-03',1500),
('2026-04',900);
CREATE TABLE `stores` (`store_id` INT, `store_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin);
INSERT INTO `stores` VALUES
(1,'古晋'),
(2,'美里'),
(3,'诗巫');
-- 参考解；等价解须从同一初始快照另行运行。
SELECT s.store_id,m.month FROM stores s CROSS JOIN monthly m ORDER BY s.store_id,m.month;
