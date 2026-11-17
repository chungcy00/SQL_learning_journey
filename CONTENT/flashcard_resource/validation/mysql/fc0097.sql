-- fc0097：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `list_a` (`value` INT) ENGINE=InnoDB;
INSERT INTO `list_a` VALUES (1), (1), (2), (NULL);
CREATE TABLE `list_b` (`value` INT) ENGINE=InnoDB;
INSERT INTO `list_b` VALUES (1), (3), (NULL);
SELECT 'A' AS src,value FROM list_a UNION ALL SELECT 'B' AS src,value FROM list_b ORDER BY src,value;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["src", "value"], "rows": [["A", null], ["A", 1], ["A", 1], ["A", 2], ["B", null], ["B", 1], ["B", 3]]}
