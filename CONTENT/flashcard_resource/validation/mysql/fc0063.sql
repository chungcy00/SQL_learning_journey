-- fc0063：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `items` (`id` INT, `name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `category` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `price` INT, `stock` INT) ENGINE=InnoDB;
INSERT INTO `items` VALUES (1, 'Tea', 'A', 12, 0), (2, 'Rice', 'A', 20, 8), (3, 'Soap', 'B', 12, 3), (4, 'Milk', 'B', NULL, 0), (5, 'Bag', 'C', 30, NULL), (6, 'Tea_2', 'A', 20, 6);
SELECT id,CASE WHEN stock=0 THEN '零' WHEN stock IS NULL THEN '未知' ELSE '其他' END AS label FROM items ORDER BY id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id", "label"], "rows": [[1, "零"], [2, "其他"], [3, "其他"], [4, "零"], [5, "未知"], [6, "其他"]]}
