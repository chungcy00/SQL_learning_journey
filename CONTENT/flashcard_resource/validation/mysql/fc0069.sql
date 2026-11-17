-- fc0069：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `scores` (`id` INT, `team` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `score` INT) ENGINE=InnoDB;
INSERT INTO `scores` VALUES (1, 'A', 90), (2, 'A', 90), (3, 'A', 70), (4, 'A', 60), (5, 'B', 80), (6, 'B', 60);
SELECT id,CASE WHEN score>=80 THEN '良好' WHEN score>=60 THEN '及格' ELSE '未及格' END AS label FROM scores ORDER BY id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id", "label"], "rows": [[1, "良好"], [2, "良好"], [3, "及格"], [4, "及格"], [5, "良好"], [6, "及格"]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
