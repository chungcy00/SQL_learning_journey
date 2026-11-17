-- fc0067：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `amounts` (`id` INT, `x` INT, `y` INT) ENGINE=InnoDB;
INSERT INTO `amounts` VALUES (1, 7, 2), (2, -7, 2), (3, 0, 0), (4, 8, 4);
SELECT id,1.0*x/NULLIF(y,0) AS ratio FROM amounts ORDER BY id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id", "ratio"], "rows": [[1, 3.5], [2, -3.5], [3, null], [4, 2.0]]}
