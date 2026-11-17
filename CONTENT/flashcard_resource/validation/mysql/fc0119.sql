-- fc0119：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `scores` (`id` INT, `team` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `score` INT) ENGINE=InnoDB;
INSERT INTO `scores` VALUES (1, 'A', 90), (2, 'A', 90), (3, 'A', 70), (4, 'A', 60), (5, 'B', 80), (6, 'B', 60);
WITH x AS (SELECT id,RANK() OVER(PARTITION BY team ORDER BY score DESC) AS r FROM scores) SELECT id,r FROM x WHERE r<=3 ORDER BY id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id", "r"], "rows": [[1, 1], [2, 1], [3, 3], [5, 1], [6, 2]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
