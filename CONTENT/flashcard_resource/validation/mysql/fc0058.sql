-- fc0058：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `stores` (`id` INT, `name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;
INSERT INTO `stores` VALUES (1, '甲店'), (2, '乙店'), (3, '丙店');
CREATE TABLE `dim` (`id` INT, `label` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;
INSERT INTO `dim` VALUES (1, '红'), (2, '蓝');
SELECT s.id,d.id AS color_id FROM stores s CROSS JOIN dim d ORDER BY s.id,d.id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id", "color_id"], "rows": [[1, 1], [1, 2], [2, 1], [2, 2], [3, 1], [3, 2]]}
