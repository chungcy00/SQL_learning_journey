-- fc0029：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `items` (`id` INT, `name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `price` INT, `stock` INT) ENGINE=InnoDB;
INSERT INTO `items` VALUES (1, 'Tea', 'A', 12, 0), (2, 'Rice', 'A', 20, 8), (3, 'Soap', 'B', 12, 3), (4, 'Milk', 'B', NULL, 0), (5, 'Bag', 'C', 30, NULL), (6, 'Tea_2', 'A', 20, 6);
SELECT id FROM items ORDER BY id LIMIT 2 OFFSET 4;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id"], "rows": [[5], [6]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
