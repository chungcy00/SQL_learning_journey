-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `stores` (`store_id` INT, `store_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `stores` VALUES (1,'古晋'),
(2,'美里'),
(3,'诗巫');

CREATE TABLE `monthly` (`month` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `sales` INT) ENGINE=InnoDB;

INSERT INTO `monthly` VALUES ('2026-01',1000),
('2026-02',1500),
('2026-03',1500),
('2026-04',900);

SELECT s.store_id,m.month FROM stores s CROSS JOIN monthly m ORDER BY s.store_id,m.month;
