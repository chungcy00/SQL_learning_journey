-- fc0108：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `tree` (`id` INT, `parent_id` INT, `label` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;
INSERT INTO `tree` VALUES (1, NULL, '总部'), (2, 1, '东区'), (3, 1, '西区'), (4, 2, '分店');
WITH RECURSIVE h(id,depth) AS (SELECT id,0 FROM tree WHERE parent_id IS NULL UNION ALL SELECT t.id,h.depth+1 FROM tree t JOIN h ON t.parent_id=h.id) SELECT id,depth FROM h ORDER BY id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id", "depth"], "rows": [[1, 0], [2, 1], [3, 1], [4, 2]]}
