-- fc0041：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `empty_values` (`id` INT, `value` INT) ENGINE=InnoDB;
SELECT COUNT(*) AS n,SUM(value) AS s,AVG(value) AS a FROM empty_values;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["n", "s", "a"], "rows": [[0, null, null]]}
