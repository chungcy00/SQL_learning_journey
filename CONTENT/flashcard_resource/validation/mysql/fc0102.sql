-- fc0102：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `list_a` (`value` INT) ENGINE=InnoDB;
INSERT INTO `list_a` VALUES (1), (1), (2), (NULL);
CREATE TABLE `list_b` (`value` INT) ENGINE=InnoDB;
INSERT INTO `list_b` VALUES (1), (3), (NULL);
SELECT value FROM list_a UNION ALL SELECT value FROM list_b ORDER BY value;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["value"], "rows": [[null], [null], [1], [1], [1], [2], [3]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
